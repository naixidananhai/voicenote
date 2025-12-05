package com.voicelife.assistant.vad

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*

/**
 * VAD检测器
 * 实时监听麦克风音频,使用Silero VAD检测人声活动
 *
 * 工作流程:
 * 1. 持续录制音频 (16kHz, MONO, 16bit PCM)
 * 2. 将音频帧送入VAD引擎分析
 * 3. 使用状态机处理检测结果(防抖动)
 * 4. 通过回调通知人声活动状态
 *
 * 防抖动策略:
 * - 连续3帧检测到人声才触发onVoiceStart
 * - 连续30帧(约2秒)静音才触发onVoiceEnd
 */
class VadDetector(
    private val context: Context,
    private val callback: VadCallback
) {
    private var audioRecord: AudioRecord? = null
    private var vadEngine: SileroVadEngine? = null

    private var isRunning = false
    private var detectionJob: Job? = null

    // 音频参数
    private val sampleRate = 16000
    private val frameSize = 512  // 每帧512样本 = 32ms
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(frameSize * 4)

    // VAD参数
    private val voiceThreshold = 0.5f  // 人声概率阈值
    private val minVoiceFrames = 3     // 连续3帧才触发开始
    private val minSilenceFrames = 30  // 约2秒静音才结束

    // 状态机
    private var consecutiveVoiceFrames = 0
    private var consecutiveSilenceFrames = 0
    private var isVoiceActive = false

    companion object {
        private const val TAG = "VadDetector"
    }

    /**
     * 初始化检测器
     * @throws SecurityException 如果缺少录音权限
     * @throws Exception 如果VAD引擎初始化失败
     */
    fun init() {
        try {
            // 初始化VAD引擎
            vadEngine = SileroVadEngine(context)
            vadEngine?.init()

            // 初始化AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord initialization failed")
            }

            Log.d(TAG, "VAD Detector initialized (buffer size: $bufferSize)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing RECORD_AUDIO permission", e)
            callback.onError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VAD Detector", e)
            callback.onError(e)
            throw e
        }
    }

    /**
     * 开始检测
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "VAD detection already running")
            return
        }

        try {
            audioRecord?.startRecording()
            isRunning = true

            detectionJob = CoroutineScope(Dispatchers.IO).launch {
                detectVoiceActivity()
            }

            Log.d(TAG, "VAD detection started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VAD detection", e)
            callback.onError(e)
        }
    }

    /**
     * 检测循环
     */
    private suspend fun detectVoiceActivity() {
        val audioBuffer = ShortArray(frameSize)
        val floatBuffer = FloatArray(frameSize)

        while (isRunning && audioRecord != null) {
            try {
                // 读取音频数据
                val readSize = audioRecord!!.read(audioBuffer, 0, frameSize)
                if (readSize <= 0) {
                    Log.w(TAG, "AudioRecord read returned: $readSize")
                    delay(10)
                    continue
                }

                // 转换为float [-1.0, 1.0]
                for (i in 0 until readSize) {
                    floatBuffer[i] = audioBuffer[i] / 32768.0f
                }

                // 如果读取的数据不足一帧,填充0
                if (readSize < frameSize) {
                    for (i in readSize until frameSize) {
                        floatBuffer[i] = 0f
                    }
                }

                // VAD处理
                val probability = vadEngine?.process(floatBuffer) ?: 0f

                // 状态机处理
                processVadResult(probability)

            } catch (e: CancellationException) {
                // 协程被取消,正常退出
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in voice detection loop", e)
                if (isRunning) {
                    callback.onError(e)
                }
                delay(100)  // 出错后短暂延迟
            }
        }

        Log.d(TAG, "Detection loop ended")
    }

    /**
     * 处理VAD结果(状态机)
     * @param probability 人声概率 [0.0, 1.0]
     */
    private fun processVadResult(probability: Float) {
        if (probability > voiceThreshold) {
            // 检测到人声
            consecutiveVoiceFrames++
            consecutiveSilenceFrames = 0

            if (!isVoiceActive && consecutiveVoiceFrames >= minVoiceFrames) {
                // 触发人声开始
                isVoiceActive = true
                callback.onVoiceStart()
                Log.d(TAG, "Voice activity started (prob: $probability)")
            }
        } else {
            // 静音
            consecutiveSilenceFrames++
            consecutiveVoiceFrames = 0

            if (isVoiceActive && consecutiveSilenceFrames >= minSilenceFrames) {
                // 触发人声结束
                isVoiceActive = false
                callback.onVoiceEnd()
                Log.d(TAG, "Voice activity ended (prob: $probability)")
            }
        }
    }

    /**
     * 停止检测
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        detectionJob?.cancel()

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        // 重置状态
        consecutiveVoiceFrames = 0
        consecutiveSilenceFrames = 0
        isVoiceActive = false
        vadEngine?.reset()

        Log.d(TAG, "VAD detection stopped")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        stop()

        try {
            audioRecord?.release()
            audioRecord = null

            vadEngine?.release()
            vadEngine = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }

        Log.d(TAG, "VAD Detector released")
    }

    /**
     * 检查是否正在运行
     */
    fun isRunning(): Boolean = isRunning
}
