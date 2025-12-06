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
    private var vadEngine: SileroVadEngine? = null

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
            vadEngine?.process(audioFrame) ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
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
