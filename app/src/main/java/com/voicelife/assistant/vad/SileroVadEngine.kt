package com.voicelife.assistant.vad

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Silero VAD引擎封装
 * 基于ONNX Runtime实现语音活动检测
 *
 * 模型信息:
 * - 大小: ~1MB
 * - 采样率: 16kHz
 * - 帧大小: 512样本
 * - 输出: 人声概率 [0.0, 1.0]
 */
class SileroVadEngine(private val context: Context) {
    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null

    private val sampleRate = 16000
    private val frameSize = 512  // 样本数

    // 模型状态 (LSTM hidden states)
    private var h: FloatBuffer? = null
    private var c: FloatBuffer? = null
    private var sr: LongBuffer? = null

    companion object {
        private const val TAG = "SileroVadEngine"
        private const val MODEL_FILENAME = "silero_vad.onnx"
    }

    /**
     * 初始化VAD引擎
     * @throws Exception 如果模型加载失败
     */
    fun init() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()

            // 从assets加载模型
            val modelBytes = context.assets.open(MODEL_FILENAME).use { it.readBytes() }

            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setIntraOpNumThreads(1)  // 单线程推理

            // 尝试使用NNAPI硬件加速
            try {
                sessionOptions.addNnapi()
                Log.d(TAG, "NNAPI acceleration enabled")
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI not available, using CPU")
            }

            ortSession = ortEnvironment!!.createSession(modelBytes, sessionOptions)

            // 初始化LSTM状态
            initializeState()

            Log.d(TAG, "Silero VAD initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Silero VAD", e)
            throw e
        }
    }

    /**
     * 初始化LSTM状态
     */
    private fun initializeState() {
        // h和c状态: [2, 1, 64]
        h = FloatBuffer.allocate(2 * 64)
        c = FloatBuffer.allocate(2 * 64)

        // 采样率参数
        sr = LongBuffer.allocate(1).apply {
            put(sampleRate.toLong())
            rewind()
        }
    }

    /**
     * 处理音频帧
     * @param audioFrame 音频数据,长度必须为512,值域为[-1.0, 1.0]
     * @return 人声概率 [0.0, 1.0], 值越高表示人声可能性越大
     */
    fun process(audioFrame: FloatArray): Float {
        require(audioFrame.size == frameSize) {
            "Audio frame size must be $frameSize, got ${audioFrame.size}"
        }

        try {
            // 准备输入张量
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                FloatBuffer.wrap(audioFrame),
                longArrayOf(1, audioFrame.size.toLong())
            )

            val hTensor = OnnxTensor.createTensor(
                ortEnvironment,
                h,
                longArrayOf(2, 1, 64)
            )

            val cTensor = OnnxTensor.createTensor(
                ortEnvironment,
                c,
                longArrayOf(2, 1, 64)
            )

            val srTensor = OnnxTensor.createTensor(
                ortEnvironment,
                sr,
                longArrayOf(1)
            )

            // 执行推理
            val inputs = mapOf(
                "input" to inputTensor,
                "h" to hTensor,
                "c" to cTensor,
                "sr" to srTensor
            )

            val output = ortSession!!.run(inputs)

            // 获取输出概率 [1, 1]
            val probability = (output[0].value as Array<*>)[0] as FloatArray
            val vadProb = probability[0]

            // 更新LSTM状态
            h = (output[1].value as FloatBuffer).apply { rewind() }
            c = (output[2].value as FloatBuffer).apply { rewind() }

            // 清理资源
            output.close()
            inputTensor.close()
            hTensor.close()
            cTensor.close()
            srTensor.close()

            return vadProb
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio frame", e)
            return 0f
        }
    }

    /**
     * 重置VAD状态
     * 在开始新的检测会话时调用
     */
    fun reset() {
        initializeState()
        Log.d(TAG, "VAD state reset")
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
            ortSession = null
            ortEnvironment = null
            h = null
            c = null
            sr = null
            Log.d(TAG, "VAD engine released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VAD engine", e)
        }
    }
}
