package com.voicelife.assistant.service

import android.app.Service
import android.content.Context
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

    @Inject
    lateinit var transcriptionService: com.voicelife.assistant.transcription.TranscriptionService

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
        
        // 保存服务运行状态
        com.voicelife.assistant.receiver.BootReceiver.saveServiceState(applicationContext, true)
        
        // 启动保活机制
        com.voicelife.assistant.utils.ServiceKeepAliveHelper.startKeepAlive(applicationContext)
        debugLogger.d(TAG, "保活机制已启动")
        
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

        // 如果录制器已经在运行，不要重启
        if (audioRecorder?.isRecording() == true) {
            debugLogger.i(TAG, "✅ 录制器已在运行，无需重启")
            return
        }

        // 检查录制器是否需要重新初始化
        if (audioRecorder == null) {
            debugLogger.w(TAG, "⚠️ 录制器为空，重新初始化...")
            audioRecorder = AudioRecorder(
                context = applicationContext,
                recordingsDir = storageManager.getRecordingsDir(),
                debugLogger = debugLogger
            )
            try {
                audioRecorder?.init()
                debugLogger.i(TAG, "✅ 录制器重新初始化成功")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reinitialize audio recorder", e)
                debugLogger.e(TAG, "❌ 录制器重新初始化失败: ${e.message}")
                notificationHelper.showWarningNotification(WarningType.PERMISSION_LOST)
                return
            }
        }

        // 启动音频录制器
        try {
            audioRecorder?.start { file ->
                onRecordingComplete(file)
            }
            debugLogger.i(TAG, "✅ 音频录制器已启动，等待人声...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recorder", e)
            debugLogger.e(TAG, "❌ 启动录制器失败: ${e.message}")
            notificationHelper.showWarningNotification(WarningType.PERMISSION_LOST)
        }

        // 启动通知更新
        startNotificationUpdater()

        // 启动定期检查
        startPeriodicChecks()

        // 启动转录服务
        serviceScope.launch {
            try {
                transcriptionService.start()
                debugLogger.i(TAG, "✅ 转录服务已启动")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start transcription service", e)
                debugLogger.e(TAG, "转录服务启动失败: ${e.message}")
            }
        }

        Log.d(TAG, "Monitoring started")
    }

    /**
     * 停止监听
     */
    private fun stopMonitoring() {
        // 保存服务停止状态
        com.voicelife.assistant.receiver.BootReceiver.saveServiceState(applicationContext, false)
        
        // 停止保活机制
        com.voicelife.assistant.utils.ServiceKeepAliveHelper.stopKeepAlive(applicationContext)
        debugLogger.d(TAG, "保活机制已停止")
        
        // 停止音频录制
        audioRecorder?.stop()

        // 停止转录服务
        serviceScope.launch {
            transcriptionService.stop()
        }

        // 停止通知更新
        updateNotificationJob?.cancel()

        // 停止服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d(TAG, "Monitoring stopped")
        debugLogger.i(TAG, "监听已停止")
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

                // 将录音加入转录队列
                transcriptionService.enqueue(recordingId)
                debugLogger.d(TAG, "📝 已加入转录队列")

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
     * 每小时检查一次存储空间，每5分钟检查一次录制器健康状态
     */
    private fun startPeriodicChecks() {
        // 存储空间检查（每小时）
        serviceScope.launch {
            while (isActive) {
                delay(60 * 60 * 1000)  // 1小时

                try {
                    // 检查存储空间
                    if (!storageManager.hasEnoughSpace()) {
                        Log.w(TAG, "Storage space low, triggering cleanup")
                        debugLogger.w(TAG, "存储空间不足，触发清理")
                        notificationHelper.showWarningNotification(WarningType.STORAGE_LOW)

                        // 执行清理
                        val result = storageManager.performCleanup()
                        Log.d(TAG, "Cleanup completed: ${result.deletedFiles} files, ${result.getFreedSpaceMB()}MB freed")
                        debugLogger.i(TAG, "清理完成: ${result.deletedFiles}个文件, ${result.getFreedSpaceMB()}MB")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic check failed", e)
                }
            }
        }
        
        // 录制器健康检查（每5分钟）
        serviceScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000)  // 5分钟

                try {
                    // 检查录制器是否健康
                    if (audioRecorder?.isRecording() == true && audioRecorder?.isHealthy() == false) {
                        Log.w(TAG, "AudioRecorder unhealthy, attempting recovery")
                        debugLogger.w(TAG, "⚠️ 检测到录制器异常，尝试恢复...")
                        
                        // 尝试重启录制器
                        audioRecorder?.stop()
                        delay(1000)
                        
                        audioRecorder?.start { file ->
                            onRecordingComplete(file)
                        }
                        
                        debugLogger.i(TAG, "✅ 录制器已重启")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Health check failed", e)
                    debugLogger.e(TAG, "健康检查失败: ${e.message}")
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed, attempting to restart service")
        debugLogger.w(TAG, "⚠️ 应用被清理，尝试重启服务...")
        
        // 如果服务应该运行，则重启
        val prefs = getSharedPreferences("voice_assistant_prefs", Context.MODE_PRIVATE)
        val shouldRun = prefs.getBoolean("service_was_running", false)
        
        if (shouldRun) {
            // 重启服务
            val restartIntent = Intent(applicationContext, VoiceMonitorService::class.java)
            VoiceMonitorService.startService(restartIntent)
            startForegroundService(restartIntent)
            debugLogger.i(TAG, "🔄 服务重启命令已发送")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        debugLogger.w(TAG, "服务被销毁")

        // 取消所有协程
        serviceScope.cancel()

        // 释放音频录制器
        audioRecorder?.release()
        audioRecorder = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
