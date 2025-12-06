package com.voicelife.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.voicelife.assistant.data.repository.RecordingRepository
import com.voicelife.assistant.recorder.AudioRecorder
import com.voicelife.assistant.storage.StorageManager
import com.voicelife.assistant.utils.NotificationHelper
import com.voicelife.assistant.utils.ServiceState
import com.voicelife.assistant.utils.WarningType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

/**
 * 语音监听前台服务
 * 协调VAD检测、录音管理、存储管理等模块
 *
 * 功能:
 * 1. 24小时运行的前台服务
 * 2. 协调VAD检测和录音
 * 3. 管理录音文件和数据库
 * 4. 监控存储空间
 * 5. 更新通知状态
 *
 * 保活策略:
 * - 前台服务(最稳定)
 * - START_STICKY重启
 * - 定期存储检查
 */
@AndroidEntryPoint
class VoiceMonitorService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var recordingRepository: RecordingRepository

    @Inject
    lateinit var storageManager: StorageManager

    @Inject
    lateinit var debugLogger: com.voicelife.assistant.utils.DebugLogger

    private var audioRecorder: AudioRecorder? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var recordingStartTime = 0L
    private var updateNotificationJob: Job? = null

    companion object {
        private const val TAG = "VoiceMonitorService"
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_STOP = "ACTION_STOP"

        fun startService(intent: Intent): Intent {
            return intent.apply { action = ACTION_START }
        }

        fun stopService(intent: Intent): Intent {
            return intent.apply { action = ACTION_STOP }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        debugLogger.i(TAG, "服务创建")

        // 初始化存储管理器
        storageManager.init()
        debugLogger.d(TAG, "存储管理器初始化完成")

        // 初始化音频录制器
        audioRecorder = AudioRecorder(
            context = applicationContext,
            recordingsDir = storageManager.getRecordingsDir(),
            debugLogger = debugLogger
        )

        try {
            audioRecorder?.init()
            debugLogger.i(TAG, "音频录制器初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio recorder", e)
            debugLogger.e(TAG, "音频录制器初始化失败: ${e.message}")
            notificationHelper.showWarningNotification(WarningType.PERMISSION_LOST)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service start command received")

        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
            else -> startMonitoring()  // 默认启动
        }

        return START_STICKY
    }

    /**
     * 开始监听
     */
    private fun startMonitoring() {
        debugLogger.i(TAG, "开始监听...")
        
        // 检查存储空间
        if (!storageManager.hasEnoughSpace()) {
            Log.w(TAG, "Insufficient storage space")
            debugLogger.w(TAG, "存储空间不足")
            notificationHelper.showWarningNotification(WarningType.STORAGE_LOW)
        }

        // 启动前台服务
        val notification = notificationHelper.createServiceNotification(ServiceState.Idle)
        startForeground(notificationHelper.getNotificationId(), notification)
        debugLogger.d(TAG, "前台服务已启动")

        // 启动音频录制器
        audioRecorder?.start { file ->
            onRecordingComplete(file)
        }
        debugLogger.i(TAG, "音频录制器已启动，等待人声...")

        // 启动通知更新
        startNotificationUpdater()

        // 启动定期检查
        startPeriodicChecks()

        Log.d(TAG, "Monitoring started")
    }

    /**
     * 停止监听
     */
    private fun stopMonitoring() {
        // 停止音频录制
        audioRecorder?.stop()

        // 停止通知更新
        updateNotificationJob?.cancel()

        // 停止服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d(TAG, "Monitoring stopped")
    }

    /**
     * 录音完成回调
     */
    private fun onRecordingComplete(file: File) {
        serviceScope.launch {
            try {
                debugLogger.i(TAG, "💾 录音完成，正在保存...")
                
                // 保存到数据库
                val recordingId = recordingRepository.saveRecording(file)
                val sizeKB = file.length() / 1024
                
                Log.d(TAG, "Recording saved: $recordingId, file: ${file.name}")
                debugLogger.i(TAG, "✅ 已保存: ${file.name} (${sizeKB}KB)")
                debugLogger.d(TAG, "录音ID: $recordingId")

                // TODO: Phase 4 - 将录音加入转换队列
                // transcriptionScheduler.enqueue(recordingId)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save recording", e)
                debugLogger.e(TAG, "保存录音失败: ${e.message}")
            }
        }
    }

    /**
     * 启动通知更新器
     * 每秒更新一次通知状态
     */
    private fun startNotificationUpdater() {
        updateNotificationJob?.cancel()
        updateNotificationJob = serviceScope.launch {
            while (isActive) {
                try {
                    val state = getCurrentState()
                    notificationHelper.updateNotification(state)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update notification", e)
                }

                delay(1000)  // 每秒更新
            }
        }
    }

    /**
     * 获取当前状态
     */
    private suspend fun getCurrentState(): ServiceState {
        return if (audioRecorder?.isSessionActive() == true) {
            val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            ServiceState.Recording(duration)
        } else {
            // TODO: Phase 4 - 获取转换队列大小
            // val queueSize = transcriptionQueue.getPendingCount()
            val queueSize = recordingRepository.getPendingRecordings().size
            if (queueSize > 0) {
                ServiceState.Processing(queueSize)
            } else {
                ServiceState.Idle
            }
        }
    }

    /**
     * 启动定期检查
     * 每小时检查一次存储空间
     */
    private fun startPeriodicChecks() {
        serviceScope.launch {
            while (isActive) {
                delay(60 * 60 * 1000)  // 1小时

                try {
                    // 检查存储空间
                    if (!storageManager.hasEnoughSpace()) {
                        Log.w(TAG, "Storage space low, triggering cleanup")
                        notificationHelper.showWarningNotification(WarningType.STORAGE_LOW)

                        // 执行清理
                        val result = storageManager.performCleanup()
                        Log.d(TAG, "Cleanup completed: ${result.deletedFiles} files, ${result.getFreedSpaceMB()}MB freed")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic check failed", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        // 取消所有协程
        serviceScope.cancel()

        // 释放音频录制器
        audioRecorder?.release()
        audioRecorder = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
