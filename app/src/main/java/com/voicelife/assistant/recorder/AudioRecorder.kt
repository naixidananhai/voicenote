package com.voicelife.assistant.recorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.voicelife.assistant.vad.VadCallback
import com.voicelife.assistant.vad.VadDetector
import kotlinx.coroutines.*
import java.io.File

/**
 * 音频录制器
 * 整合VAD检测和录音会话管理
 *
 * 工作流程:
 * 1. 启动VAD检测器监听麦克风
 * 2. VAD检测到人声 -> 触发录音会话
 * 3. 持续录制音频并写入WAV文件
 * 4. VAD检测到静音 -> 智能合并判断是否停止
 * 5. 录音完成 -> 通知回调
 *
 * 存储结构:
 * - pending/    待转换的音频
 * - processing/ 转换中的音频
 * - completed/  已转换(保留7天)
 * - failed/     转换失败
 */
class AudioRecorder(
    private val context: Context,
    private val recordingsDir: File,
    private val debugLogger: com.voicelife.assistant.utils.DebugLogger? = null
) {
    private var vadDetector: VadDetector? = null
    private var recordingSession: RecordingSession? = null
    private var audioRecord: AudioRecord? = null

    private var isRecording = false
    private var audioRecordJob: Job? = null

    // 音频参数
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val frameSize = 512  // 与VAD一致

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    ).coerceAtLeast(frameSize * 4)

    // 录音完成回调
    private var onRecordingComplete: ((File) -> Unit)? = null

    companion object {
        private const val TAG = "AudioRecorder"
    }

    /**
     * VAD检测回调
     */
    private val vadCallback = object : VadCallback {
        override fun onVoiceStart() {
            Log.d(TAG, "VAD: Voice detected")
            debugLogger?.i(TAG, "🎤 检测到人声，开始录音")
            recordingSession?.onVoiceStart()
        }

        override fun onVoiceEnd() {
            Log.d(TAG, "VAD: Voice ended")
            debugLogger?.i(TAG, "🔇 人声结束，等待静音确认...")
            recordingSession?.onVoiceEnd()
        }

        override fun onError(error: Exception) {
            Log.e(TAG, "VAD error", error)
            debugLogger?.e(TAG, "VAD错误: ${error.message}")
            // 可以在这里添加错误处理逻辑
        }
    }

    /**
     * 初始化录制器
     */
    fun init() {
        try {
            debugLogger?.d(TAG, "初始化录制器...")
            // 创建目录结构
            File(recordingsDir, "pending").mkdirs()
            File(recordingsDir, "processing").mkdirs()
            File(recordingsDir, "completed").mkdirs()
            File(recordingsDir, "failed").mkdirs()

            // 初始化VAD检测器
            debugLogger?.d(TAG, "开始初始化VAD检测器...")
            try {
                vadDetector = VadDetector(context, vadCallback)
                vadDetector?.init()
                debugLogger?.i(TAG, "✅ VAD检测器初始化成功")
            } catch (e: Exception) {
                debugLogger?.e(TAG, "❌ VAD初始化失败: ${e.javaClass.simpleName}: ${e.message}")
                debugLogger?.e(TAG, "堆栈: ${e.stackTraceToString().take(200)}")
                throw e
            }

            // 初始化AudioRecord
            debugLogger?.d(TAG, "开始初始化AudioRecord...")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord initialization failed")
            }

            Log.d(TAG, "Audio recorder initialized")
            debugLogger?.i(TAG, "✅ 录制器初始化成功 (16kHz, MONO)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio recorder", e)
            debugLogger?.e(TAG, "录制器初始化失败: ${e.message}")
            throw e
        }
    }

    /**
     * 开始录制
     * @param onComplete 录音完成回调
     */
    fun start(onComplete: (File) -> Unit) {
        if (isRecording) {
            Log.w(TAG, "Audio recorder already running")
            debugLogger?.w(TAG, "录制器已在运行")
            return
        }

        try {
            debugLogger?.i(TAG, "启动录制器...")
            this.onRecordingComplete = onComplete

            // 创建录音会话
            recordingSession = RecordingSession(recordingsDir, debugLogger) { file ->
                onRecordingComplete?.invoke(file)
            }

            // 启动音频录制
            audioRecord?.startRecording()
            isRecording = true
            debugLogger?.d(TAG, "AudioRecord已启动")

            // 启动音频读取协程
            audioRecordJob = CoroutineScope(Dispatchers.IO).launch {
                readAudioData()
            }

            debugLogger?.i(TAG, "VAD检测已启动，监听中...")
            Log.d(TAG, "Audio recorder started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recorder", e)
            debugLogger?.e(TAG, "启动录制器失败: ${e.message}")
            isRecording = false
            throw e
        }
    }

    /**
     * 读取音频数据
     * 同时送给VAD检测和录音会话
     */
    private suspend fun readAudioData() {
        val audioBuffer = ShortArray(frameSize)
        val floatBuffer = FloatArray(frameSize)
        var frameCount = 0

        debugLogger?.d(TAG, "开始读取音频数据...")

        while (isRecording && audioRecord != null) {
            try {
                // 读取音频数据
                val readSize = audioRecord!!.read(audioBuffer, 0, frameSize)
                if (readSize <= 0) {
                    Log.w(TAG, "AudioRecord read returned: $readSize")
                    debugLogger?.w(TAG, "音频读取失败: $readSize")
                    delay(10)
                    continue
                }

                frameCount++
                
                // 每100帧输出一次日志
                if (frameCount % 100 == 0) {
                    debugLogger?.d(TAG, "已处理 $frameCount 帧音频")
                }

                // 转换为float给VAD使用
                for (i in 0 until readSize) {
                    floatBuffer[i] = audioBuffer[i] / 32768.0f
                }

                // 送给VAD检测
                val probability = vadDetector?.processFrame(floatBuffer) ?: 0f
                
                // 每50帧输出一次VAD概率
                if (frameCount % 50 == 0) {
                    debugLogger?.d(TAG, "VAD概率: ${String.format("%.3f", probability)}")
                }
                
                // 根据VAD结果触发回调
                vadDetector?.handleVadResult(probability)

                // 写入录音会话(始终写入,用于预缓冲)
                recordingSession?.writeAudioData(audioBuffer)

            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error reading audio data", e)
                debugLogger?.e(TAG, "读取音频错误: ${e.message}")
                delay(100)
            }
        }

        Log.d(TAG, "Audio reading stopped")
        debugLogger?.i(TAG, "音频读取已停止，共处理 $frameCount 帧")
    }

    /**
     * 停止录制
     */
    fun stop() {
        if (!isRecording) return

        Log.d(TAG, "Stopping audio recorder...")

        isRecording = false

        // 停止音频读取
        audioRecordJob?.cancel()

        // 停止AudioRecord
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        // 重置VAD状态
        vadDetector?.reset()

        // 强制停止当前录音
        recordingSession?.forceStop()
        recordingSession = null

        Log.d(TAG, "Audio recorder stopped")
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()

        try {
            audioRecord?.release()
            audioRecord = null

            vadDetector?.release()
            vadDetector = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio recorder", e)
        }

        Log.d(TAG, "Audio recorder released")
    }

    /**
     * 检查是否正在录制
     */
    fun isRecording(): Boolean = isRecording

    /**
     * 获取当前录音会话状态
     */
    fun isSessionActive(): Boolean = recordingSession?.isRecording() ?: false
}
