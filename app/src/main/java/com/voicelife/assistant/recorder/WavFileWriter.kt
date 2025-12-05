package com.voicelife.assistant.recorder

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV文件写入器
 * 将16bit PCM音频数据写入WAV格式文件
 *
 * WAV文件格式:
 * - RIFF头 (12字节)
 * - fmt块 (24字节)
 * - data块 (8字节 + 实际数据)
 *
 * 音频参数:
 * - 采样率: 16kHz
 * - 声道数: 1 (单声道)
 * - 位深度: 16bit
 */
class WavFileWriter(private val outputFile: File) {
    private val sampleRate = 16000
    private val channels = 1
    private val bitsPerSample = 16

    private var fileOutputStream: FileOutputStream? = null
    private var dataSize = 0L

    companion object {
        private const val TAG = "WavFileWriter"
        private const val WAV_HEADER_SIZE = 44
    }

    /**
     * 开始写入
     * 创建文件并写入WAV头(占位)
     */
    fun start() {
        try {
            // 确保父目录存在
            outputFile.parentFile?.mkdirs()

            fileOutputStream = FileOutputStream(outputFile)

            // 先写入占位的WAV头(稍后更新)
            writeWavHeader(0)

            dataSize = 0
            Log.d(TAG, "Started writing WAV file: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WAV writer", e)
            throw e
        }
    }

    /**
     * 写入音频数据
     * @param audioData PCM音频样本 (16bit signed)
     */
    fun write(audioData: ShortArray) {
        try {
            val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            for (sample in audioData) {
                byteBuffer.putShort(sample)
            }

            fileOutputStream?.write(byteBuffer.array())
            dataSize += audioData.size * 2L

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write audio data", e)
            throw e
        }
    }

    /**
     * 停止写入
     * 更新WAV头并关闭文件
     */
    fun stop() {
        try {
            fileOutputStream?.close()
            fileOutputStream = null

            // 更新WAV头with正确的文件大小
            updateWavHeader()

            Log.d(TAG, "Stopped writing WAV file: ${outputFile.name}, size: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop WAV writer", e)
        }
    }

    /**
     * 写入WAV头
     * @param dataLength 数据长度(字节)
     */
    private fun writeWavHeader(dataLength: Long) {
        val header = ByteBuffer.allocate(WAV_HEADER_SIZE)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        header.put("RIFF".toByteArray())  // ChunkID
        header.putInt((36 + dataLength).toInt())  // ChunkSize
        header.put("WAVE".toByteArray())  // Format

        // fmt sub-chunk
        header.put("fmt ".toByteArray())  // Subchunk1ID
        header.putInt(16)  // Subchunk1Size (PCM)
        header.putShort(1)  // AudioFormat (PCM = 1)
        header.putShort(channels.toShort())  // NumChannels
        header.putInt(sampleRate)  // SampleRate
        header.putInt(sampleRate * channels * bitsPerSample / 8)  // ByteRate
        header.putShort((channels * bitsPerSample / 8).toShort())  // BlockAlign
        header.putShort(bitsPerSample.toShort())  // BitsPerSample

        // data sub-chunk
        header.put("data".toByteArray())  // Subchunk2ID
        header.putInt(dataLength.toInt())  // Subchunk2Size

        fileOutputStream?.write(header.array())
    }

    /**
     * 更新WAV头
     * 在文件写入完成后,更新RIFF和data块的大小字段
     */
    private fun updateWavHeader() {
        try {
            RandomAccessFile(outputFile, "rw").use { raf ->
                // 更新RIFF chunk size (文件大小 - 8)
                raf.seek(4)
                raf.write(intToLittleEndian((36 + dataSize).toInt()))

                // 更新data chunk size
                raf.seek(40)
                raf.write(intToLittleEndian(dataSize.toInt()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }

    /**
     * 将整数转换为小端字节数组
     */
    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    /**
     * 获取当前已写入的数据大小
     */
    fun getDataSize(): Long = dataSize

    /**
     * 检查是否正在写入
     */
    fun isWriting(): Boolean = fileOutputStream != null
}
