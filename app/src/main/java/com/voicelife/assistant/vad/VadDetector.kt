package com.voicelife.assistant.vad

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.voicelife.assistant.config.AudioConfig
import kotlinx.coroutines.*
import kotlin.math.sqrt

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
 * 参数配置: 见 AudioConfig.VAD
 */
class VadDetector(
    private val context: Context,
    private val callback: VadCallback
) {
    private var vadEngine: SileroVadEngine? = null

    // VAD参数 - 从配置文件读取
    private val voiceThreshold = AudioConfig.VAD.VOICE_THRESHOLD
    private val minVoiceFrames = AudioConfig.VAD.MIN_VOICE_FRAMES
    private val minSilenceFrames = AudioConfig.VAD.MIN_SILENCE_FRAMES
    private val energyThreshold = AudioConfig.VAD.ENERGY_THRESHOLD
    private val enableEnergyFilter = AudioConfig.VAD.ENABLE_ENERGY_FILTER

    // 状态机
    private var consecutiveVoiceFrames = 0
    private var consecutiveSilenceFrames = 0
    private var isVoiceActive = false

    companion object {
        private const val TAG = "VadDetector"
    }

    /**
     * 初始化检测器
     * @throws Exception 如果VAD引擎初始化失败
     */
    fun init() {
        try {
            // 初始化VAD引擎
            vadEngine = SileroVadEngine(context)
            vadEngine?.init()

            Log.d(TAG, "VAD Detector initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VAD Detector", e)
            callback.onError(e)
            throw e
        }
    }

    /**
     * 处理音频帧
     * 由外部AudioRecorder调用
     */
    fun processFrame(audioFrame: FloatArray): Float {
        return try {
            if (vadEngine == null) {
                Log.e(TAG, "VAD引擎未初始化！")
                return 0f
            }
            
            // 能量预过滤：低能量音频直接返回0
            if (enableEnergyFilter) {
                val energy = calculateRmsEnergy(audioFrame)
                if (energy < energyThreshold) {
                    return 0f  // 能量太低，直接判定为静音
                }
            }
            
            vadEngine?.process(audioFrame) ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}", e)
            0f
        }
    }

    /**
     * 处理VAD结果
     * 由外部AudioRecorder调用
     */
    fun handleVadResult(probability: Float) {
        processVadResult(probability)
    }

    /**
     * 计算音频帧的RMS能量
     */
    private fun calculateRmsEnergy(audioFrame: FloatArray): Float {
        var sum = 0f
        for (sample in audioFrame) {
            sum += sample * sample
        }
        return sqrt(sum / audioFrame.size)
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
                Log.i(TAG, "🎤 Voice activity started (prob: $probability, threshold: $voiceThreshold)")
            } else if (isVoiceActive && consecutiveVoiceFrames % 50 == 0) {
                Log.d(TAG, "Voice continues (prob: $probability, frames: $consecutiveVoiceFrames)")
            }
        } else {
            // 静音
            consecutiveSilenceFrames++
            consecutiveVoiceFrames = 0

            if (isVoiceActive && consecutiveSilenceFrames >= minSilenceFrames) {
                // 触发人声结束
                isVoiceActive = false
                callback.onVoiceEnd()
                Log.i(TAG, "🔇 Voice activity ended (prob: $probability)")
            } else if (isVoiceActive && consecutiveSilenceFrames % 10 == 0) {
                Log.d(TAG, "Silence detected (prob: $probability, frames: $consecutiveSilenceFrames/$minSilenceFrames)")
            }
        }
    }

    /**
     * 重置状态
     */
    fun reset() {
        consecutiveVoiceFrames = 0
        consecutiveSilenceFrames = 0
        isVoiceActive = false
        vadEngine?.reset()
        Log.d(TAG, "VAD state reset")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        try {
            vadEngine?.release()
            vadEngine = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }

        Log.d(TAG, "VAD Detector released")
    }
}
