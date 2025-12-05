package com.voicelife.assistant.data.repository

import com.voicelife.assistant.data.database.RecordingDao
import com.voicelife.assistant.data.database.TranscriptionDao
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.model.Transcription
import com.voicelife.assistant.data.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 录音数据仓库
 * 统一管理录音和转写数据的访问
 */
@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao,
    private val transcriptionDao: TranscriptionDao
) {

    /**
     * 保存新录音记录
     * @param file 录音文件
     * @return 录音ID
     */
    suspend fun saveRecording(file: File): Long {
        val now = System.currentTimeMillis()
        val deleteAt = now + TimeUnit.DAYS.toMillis(7)  // 7天后删除

        val recording = Recording(
            filePath = file.absolutePath,
            duration = calculateDuration(file),
            fileSize = file.length(),
            createdAt = now,
            transcriptionStatus = TranscriptionStatus.PENDING,
            deleteAt = deleteAt
        )

        return recordingDao.insert(recording)
    }

    /**
     * 更新录音记录
     */
    suspend fun updateRecording(recording: Recording) {
        recordingDao.update(recording)
    }

    /**
     * 删除录音记录
     */
    suspend fun deleteRecording(recording: Recording) {
        // 删除文件
        File(recording.filePath).delete()

        // 删除数据库记录
        recordingDao.delete(recording)
    }

    /**
     * 根据ID获取录音
     */
    suspend fun getRecordingById(id: Long): Recording? {
        return recordingDao.getById(id)
    }

    /**
     * 获取所有录音(Flow)
     */
    fun getAllRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllFlow()
    }

    /**
     * 根据状态获取录音
     */
    suspend fun getRecordingsByStatus(status: TranscriptionStatus): List<Recording> {
        return recordingDao.getByStatus(status)
    }

    /**
     * 获取待转换的录音
     */
    suspend fun getPendingRecordings(): List<Recording> {
        return recordingDao.getByStatus(TranscriptionStatus.PENDING)
    }

    /**
     * 更新转换状态
     */
    suspend fun updateTranscriptionStatus(
        recordingId: Long,
        status: TranscriptionStatus,
        transcribedAt: Long? = null
    ) {
        val recording = recordingDao.getById(recordingId) ?: return

        val updated = recording.copy(
            transcriptionStatus = status,
            transcribedAt = transcribedAt ?: recording.transcribedAt
        )

        recordingDao.update(updated)
    }

    /**
     * 保存转写结果
     */
    suspend fun saveTranscription(
        recordingId: Long,
        text: String,
        language: String?,
        segments: String
    ): Long {
        val transcription = Transcription(
            recordingId = recordingId,
            text = text,
            language = language,
            segments = segments,
            createdAt = System.currentTimeMillis()
        )

        val id = transcriptionDao.insert(transcription)

        // 更新录音状态
        updateTranscriptionStatus(
            recordingId,
            TranscriptionStatus.COMPLETED,
            System.currentTimeMillis()
        )

        return id
    }

    /**
     * 根据录音ID获取转写
     */
    suspend fun getTranscriptionByRecordingId(recordingId: Long): Transcription? {
        return transcriptionDao.getByRecordingId(recordingId)
    }

    /**
     * 搜索转写文本
     */
    fun searchTranscriptions(keyword: String): Flow<List<Transcription>> {
        return transcriptionDao.searchByKeyword(keyword)
    }

    /**
     * 根据日期范围获取转写
     */
    suspend fun getTranscriptionsByDateRange(
        startTime: Long,
        endTime: Long
    ): List<Transcription> {
        return transcriptionDao.getByDateRange(startTime, endTime)
    }

    /**
     * 获取过期的录音
     */
    suspend fun getExpiredRecordings(): List<Recording> {
        val now = System.currentTimeMillis()
        return recordingDao.getExpiredRecordings(now)
    }

    /**
     * 删除过期的录音
     */
    suspend fun deleteExpiredRecordings(): Int {
        val expired = getExpiredRecordings()

        // 删除文件
        expired.forEach { recording ->
            File(recording.filePath).delete()
        }

        // 删除数据库记录
        val now = System.currentTimeMillis()
        return recordingDao.deleteExpired(now)
    }

    /**
     * 计算录音时长(秒)
     */
    private fun calculateDuration(file: File): Int {
        // WAV文件大小 = 44字节头 + 数据
        // 数据大小 = 采样率 * 声道数 * 位深度/8 * 时长
        // 时长 = (文件大小 - 44) / (16000 * 1 * 2)
        val dataSize = file.length() - 44
        return (dataSize / (16000 * 2)).toInt().coerceAtLeast(0)
    }

    /**
     * 获取统计信息
     */
    suspend fun getStatistics(): RecordingStatistics {
        val allRecordings = getRecordingsByStatus(TranscriptionStatus.COMPLETED) +
                getRecordingsByStatus(TranscriptionStatus.PENDING) +
                getRecordingsByStatus(TranscriptionStatus.PROCESSING) +
                getRecordingsByStatus(TranscriptionStatus.FAILED)

        val totalCount = allRecordings.size
        val totalDuration = allRecordings.sumOf { it.duration }
        val totalSize = allRecordings.sumOf { it.fileSize }
        val pendingCount = allRecordings.count { it.transcriptionStatus == TranscriptionStatus.PENDING }

        return RecordingStatistics(
            totalCount = totalCount,
            totalDurationSeconds = totalDuration,
            totalSizeBytes = totalSize,
            pendingCount = pendingCount
        )
    }
}

/**
 * 录音统计信息
 */
data class RecordingStatistics(
    val totalCount: Int,
    val totalDurationSeconds: Int,
    val totalSizeBytes: Long,
    val pendingCount: Int
) {
    fun getTotalDurationMinutes(): Int = totalDurationSeconds / 60
    fun getTotalSizeMB(): Long = totalSizeBytes / (1024 * 1024)
}
