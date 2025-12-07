package com.voicelife.assistant.transcription

import android.content.Context
import android.util.Log
import com.voicelife.assistant.data.model.TranscriptionStatus
import com.voicelife.assistant.data.repository.RecordingRepository
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 转录服务
 * 负责管理录音文件的转录队列和处理
 */
@Singleton
class TranscriptionService @Inject constructor(
    private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val debugLogger: com.voicelife.assistant.utils.DebugLogger
) {

    private val whisperEngine = WhisperEngine(context)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isProcessing = false
    private var processingJob: Job? = null

    companion object {
        private const val TAG = "TranscriptionService"
        private const val PROCESS_INTERVAL = 5000L  // 每5秒检查一次队列
    }

    /**
     * 启动转录服务
     */
    suspend fun start() {
        if (isProcessing) {
            Log.d(TAG, "Transcription service already running")
            return
        }

        debugLogger.i(TAG, "启动转录服务...")

        // 初始化Whisper引擎
        val modelPath = whisperEngine.getModelPath()
        if (!whisperEngine.isModelAvailable(modelPath)) {
            debugLogger.w(TAG, "模型文件不存在，尝试从assets复制...")
            val copied = whisperEngine.copyModelFromAssets()
            if (!copied) {
                debugLogger.e(TAG, "❌ 无法复制模型文件")
                return
            }
        }

        val initialized = whisperEngine.initialize(modelPath)
        if (!initialized) {
            debugLogger.e(TAG, "❌ Whisper引擎初始化失败")
            return
        }

        debugLogger.i(TAG, "✅ Whisper引擎初始化成功")

        // 启动处理循环
        isProcessing = true
        debugLogger.i(TAG, "🚀 启动转录队列处理循环...")
        
        processingJob = serviceScope.launch {
            debugLogger.i(TAG, "📍 转录处理协程已启动")
            while (isActive && isProcessing) {
                try {
                    processQueue()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing queue", e)
                    debugLogger.e(TAG, "处理队列出错: ${e.message}")
                }
                delay(PROCESS_INTERVAL)
            }
            debugLogger.i(TAG, "📍 转录处理协程已退出")
        }

        debugLogger.i(TAG, "✅ 转录服务已启动")
    }

    /**
     * 停止转录服务
     */
    suspend fun stop() {
        isProcessing = false
        processingJob?.cancel()
        whisperEngine.release()
        debugLogger.i(TAG, "转录服务已停止")
    }

    /**
     * 添加录音到转录队列
     */
    suspend fun enqueue(recordingId: Long) {
        try {
            // 更新状态为待处理（保持PENDING状态，等待队列处理）
            debugLogger.d(TAG, "录音 $recordingId 已加入转录队列")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue recording", e)
            debugLogger.e(TAG, "加入队列失败: ${e.message}")
        }
    }

    /**
     * 处理转录队列
     */
    private suspend fun processQueue() {
        try {
            debugLogger.d(TAG, "🔍 检查转录队列...")
            
            // 获取待处理的录音
            val pendingRecordings = recordingRepository.getPendingRecordings()
            debugLogger.d(TAG, "📋 待处理录音数量: ${pendingRecordings.size}")
            
            if (pendingRecordings.isEmpty()) {
                return
            }

            debugLogger.d(TAG, "队列中有 ${pendingRecordings.size} 个待转录文件")

            // 逐个处理
            for (recording in pendingRecordings) {
                if (!isProcessing) break

                try {
                    debugLogger.i(TAG, "🎯 开始转录: ${File(recording.filePath).name}")

                    // 更新状态为处理中
                    recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.PROCESSING)

                    // 执行转录
                    val text = whisperEngine.transcribe(recording.filePath)

                    if (text != null && text.isNotEmpty()) {
                        // 转录成功，保存文本
                        recordingRepository.updateTranscription(recording.id, text)
                        
                        debugLogger.i(TAG, "✅ 转录完成: ${text.take(50)}${if (text.length > 50) "..." else ""}")
                    } else {
                        // 转录失败或无内容
                        recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.FAILED)
                        debugLogger.w(TAG, "⚠️ 转录失败或无内容")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to transcribe recording ${recording.id}", e)
                    debugLogger.e(TAG, "转录失败: ${e.message}")
                    recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.FAILED)
                }

                // 短暂延迟，避免CPU占用过高
                delay(1000)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in processQueue", e)
            debugLogger.e(TAG, "处理队列出错: ${e.message}")
        }
    }

    /**
     * 获取队列大小
     */
    suspend fun getQueueSize(): Int {
        return try {
            recordingRepository.getPendingRecordings().size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 重试失败的转录
     */
    suspend fun retryFailed() {
        try {
            val failedRecordings = recordingRepository.getFailedRecordings()
            debugLogger.i(TAG, "重试 ${failedRecordings.size} 个失败的转录")
            
            for (recording in failedRecordings) {
                recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.PENDING)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry failed transcriptions", e)
            debugLogger.e(TAG, "重试失败: ${e.message}")
        }
    }
}
