package com.voicelife.assistant.recorder

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音会话管理器
 * 负责智能合并对话段落,避免短暂停顿导致录音分段
 *
 * 智能合并策略:
 * - 预缓冲: 保留最近1秒音频,人声开始时写入
 * - 静音间隔: 10秒内的静音视为短暂停顿,继续录音
 * - 后缓冲: 人声结束后继续录制3秒
 *
 * 文件命名: voice_yyyyMMdd_HHmmss.wav
 */
class RecordingSession(
    private val recordingsDir: File,
    private val debugLogger: com.voicelife.assistant.utils.DebugLogger? = null,
    private val onRecordingComplete: (File) -> Unit
) {
    private var wavWriter: WavFileWriter? = null
    private var currentFile: File? = null
    private var lastVoiceTime = 0L
    private var silenceCheckJob: Job? = null
    private var postBufferJob: Job? = null

    private val preBuffer = RingBuffer(16000 * 2)  // 1秒预缓冲 (16kHz * 2 bytes)
    private val silenceGapMs = 10000L  // 10秒静音间隔
    private val postBufferMs = 3000L  // 3秒后缓冲

    companion object {
        private const val TAG = "RecordingSession"
        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    /**
     * 人声开始
     * 如果是新录音,创建文件并写入预缓冲数据
     * 如果正在录音,重置静音计时
     */
    fun onVoiceStart() {
        if (wavWriter != null) {
            // 已经在录音中,重置静音计时
            lastVoiceTime = System.currentTimeMillis()
            silenceCheckJob?.cancel()
            Log.d(TAG, "Voice continues, silence check cancelled")
            debugLogger?.d(TAG, "人声继续，取消静音检查")
            return
        }

        // 开始新的录音
        val timestamp = dateFormat.format(Date())
        currentFile = File(recordingsDir, "pending/voice_$timestamp.wav")
        currentFile?.parentFile?.mkdirs()

        wavWriter = WavFileWriter(currentFile!!)
        wavWriter?.start()

        // 写入预缓冲数据
        val preBufferData = preBuffer.read()
        if (preBufferData.isNotEmpty()) {
            wavWriter?.write(preBufferData)
        }

        lastVoiceTime = System.currentTimeMillis()
        Log.d(TAG, "Recording started: ${currentFile?.name}")
        debugLogger?.i(TAG, "📝 开始录音: ${currentFile?.name}")
    }

    /**
     * 写入音频数据
     * 同时更新预缓冲
     */
    fun writeAudioData(audioData: ShortArray) {
        // 更新预缓冲(始终保留最近1秒)
        preBuffer.write(audioData)

        // 如果正在录音,写入文件
        wavWriter?.write(audioData)
    }

    /**
     * 人声结束
     * 启动静音检查计时器
     */
    fun onVoiceEnd() {
        lastVoiceTime = System.currentTimeMillis()

        // 开始检查静音间隔
        silenceCheckJob?.cancel()
        silenceCheckJob = CoroutineScope(Dispatchers.IO).launch {
            delay(silenceGapMs)

            // 10秒后仍无人声,停止录音
            debugLogger?.i(TAG, "⏱️ 静音超过10秒，准备停止录音")
            stopRecording()
        }

        Log.d(TAG, "Voice ended, silence check started")
        debugLogger?.d(TAG, "人声结束，启动10秒静音检查")
    }

    /**
     * 停止录音
     * 继续录制后缓冲,然后关闭文件
     */
    private fun stopRecording() {
        val file = currentFile ?: return
        val writer = wavWriter ?: return

        Log.d(TAG, "Stopping recording...")
        debugLogger?.i(TAG, "⏹️ 停止录音，录制后缓冲3秒...")

        // 继续录制后缓冲(3秒)
        postBufferJob?.cancel()
        postBufferJob = CoroutineScope(Dispatchers.IO).launch {
            delay(postBufferMs)

            // 停止写入
            writer.stop()
            wavWriter = null

            val duration = calculateDuration(file)
            val sizeKB = file.length() / 1024
            Log.d(TAG, "Recording completed: ${file.name}, size: ${file.length()} bytes, duration: ${duration}s")
            debugLogger?.i(TAG, "✨ 录音完成: ${file.name}")
            debugLogger?.d(TAG, "  时长: ${duration}秒, 大小: ${sizeKB}KB")

            // 通知录音完成
            onRecordingComplete(file)

            currentFile = null
        }
    }

    /**
     * 强制停止录音
     * 不等待后缓冲,立即关闭
     */
    fun forceStop() {
        silenceCheckJob?.cancel()
        postBufferJob?.cancel()

        wavWriter?.stop()
        wavWriter = null

        currentFile?.let {
            Log.d(TAG, "Force stopped: ${it.name}")
        }
        currentFile = null
    }

    /**
     * 计算录音时长(秒)
     */
    private fun calculateDuration(file: File): Int {
        // WAV文件大小 = 44字节头 + 数据
        // 数据大小 = 采样率 * 声道数 * 位深度/8 * 时长
        // 时长 = (文件大小 - 44) / (16000 * 1 * 2)
        val dataSize = file.length() - 44
        return (dataSize / (16000 * 2)).toInt()
    }

    /**
     * 检查是否正在录音
     */
    fun isRecording(): Boolean = wavWriter != null
}

/**
 * 环形缓冲区
 * 用于保存预缓冲数据
 */
class RingBuffer(private val capacity: Int) {
    private val buffer = ShortArray(capacity)
    private var writePos = 0
    private var size = 0

    /**
     * 写入数据
     */
    fun write(data: ShortArray) {
        for (sample in data) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % capacity

            if (size < capacity) {
                size++
            }
        }
    }

    /**
     * 读取所有数据
     * 按照时间顺序返回
     */
    fun read(): ShortArray {
        if (size == 0) return ShortArray(0)

        val result = ShortArray(size)

        // 如果缓冲区未满,从0开始读取
        if (size < capacity) {
            System.arraycopy(buffer, 0, result, 0, size)
        } else {
            // 缓冲区已满,从writePos开始读取(最旧的数据)
            val firstPart = capacity - writePos
            System.arraycopy(buffer, writePos, result, 0, firstPart)
            System.arraycopy(buffer, 0, result, firstPart, writePos)
        }

        return result
    }

    /**
     * 清空缓冲区
     */
    fun clear() {
        writePos = 0
        size = 0
    }
}
