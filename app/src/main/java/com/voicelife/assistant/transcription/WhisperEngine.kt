package com.voicelife.assistant.transcription

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Whisper语音识别引擎
 * 使用whisper.cpp进行本地语音转文字
 */
class WhisperEngine(private val context: Context) {

    private var whisperContext: WhisperContext? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "WhisperEngine"
        private const val MODEL_NAME = "ggml-base.bin"  // 使用base模型
    }

    /**
     * 初始化Whisper引擎
     * @param modelPath 模型文件路径
     */
    suspend fun initialize(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                Log.d(TAG, "Whisper already initialized")
                return@withContext true
            }

            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return@withContext false
            }

            Log.d(TAG, "Loading Whisper model from: $modelPath")
            whisperContext = WhisperContext.createContextFromFile(modelPath)
            isInitialized = true
            
            Log.i(TAG, "✅ Whisper engine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Whisper engine", e)
            false
        }
    }

    /**
     * 转录音频文件
     * @param audioFilePath WAV音频文件路径
     * @return 转录文本，失败返回null
     */
    suspend fun transcribe(audioFilePath: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized || whisperContext == null) {
                Log.e(TAG, "❌ Whisper engine not initialized")
                return@withContext null
            }

            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "❌ Audio file not found: $audioFilePath")
                return@withContext null
            }

            Log.i(TAG, "🎯 开始转录: ${audioFile.name}")
            val startTime = System.currentTimeMillis()

            // 读取音频数据
            Log.d(TAG, "📖 读取音频文件...")
            val audioData = readWavFile(audioFile)
            if (audioData == null) {
                Log.e(TAG, "❌ Failed to read audio data")
                return@withContext null
            }
            
            val audioSeconds = audioData.size / 16000.0
            Log.i(TAG, "📊 音频信息: ${audioData.size} 样本, ${String.format("%.1f", audioSeconds)} 秒")

            // 执行转录
            Log.i(TAG, "🔄 开始 Whisper 推理... (这可能需要一些时间)")
            val result = whisperContext?.transcribeData(audioData, false)
            val duration = System.currentTimeMillis() - startTime
            
            Log.i(TAG, "⏱️ Whisper 推理完成，耗时: ${duration}ms (${String.format("%.1f", duration/1000.0)}秒)")

            if (result != null && result.isNotEmpty()) {
                Log.i(TAG, "✅ 转录成功!")
                Log.i(TAG, "📝 转录结果: ${result.take(200)}${if (result.length > 200) "..." else ""}")
                result
            } else {
                Log.w(TAG, "⚠️ 转录返回空结果")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 转录失败: ${e.message}", e)
            null
        }
    }

    /**
     * 读取WAV文件数据
     * 返回16kHz单声道的float数组
     */
    private fun readWavFile(file: File): FloatArray? {
        try {
            val bytes = file.readBytes()
            
            // 跳过WAV文件头（44字节）
            val headerSize = 44
            if (bytes.size <= headerSize) {
                Log.e(TAG, "Invalid WAV file: too small")
                return null
            }

            // 读取音频数据（16位PCM）
            val audioBytes = bytes.copyOfRange(headerSize, bytes.size)
            val samples = audioBytes.size / 2
            val audioData = FloatArray(samples)

            // 转换为float数组（归一化到-1.0到1.0）
            for (i in 0 until samples) {
                val index = i * 2
                val sample = ((audioBytes[index + 1].toInt() shl 8) or 
                             (audioBytes[index].toInt() and 0xFF)).toShort()
                audioData[i] = sample / 32768.0f
            }

            Log.d(TAG, "Read ${audioData.size} samples from WAV file")
            return audioData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read WAV file", e)
            return null
        }
    }

    /**
     * 检查模型是否存在
     */
    fun isModelAvailable(modelPath: String): Boolean {
        return File(modelPath).exists()
    }

    /**
     * 获取模型文件路径
     */
    fun getModelPath(): String {
        return File(context.filesDir, MODEL_NAME).absolutePath
    }

    /**
     * 从assets复制模型文件
     */
    suspend fun copyModelFromAssets(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(getModelPath())
            if (modelFile.exists()) {
                Log.d(TAG, "Model file already exists")
                return@withContext true
            }

            Log.d(TAG, "Copying model from assets...")
            context.assets.open(MODEL_NAME).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "✅ Model copied successfully: ${modelFile.length() / 1024 / 1024}MB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets", e)
            false
        }
    }

    /**
     * 释放资源
     */
    suspend fun release() = withContext(Dispatchers.IO) {
        try {
            whisperContext?.release()
            whisperContext = null
            isInitialized = false
            Log.d(TAG, "Whisper engine released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release Whisper engine", e)
        }
    }
}
