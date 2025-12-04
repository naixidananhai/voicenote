package com.voicelife.assistant.data.database

import androidx.room.*
import com.voicelife.assistant.data.model.Transcription
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Insert
    suspend fun insert(transcription: Transcription): Long

    @Query("SELECT * FROM transcriptions WHERE recordingId = :recordingId")
    suspend fun getByRecordingId(recordingId: Long): Transcription?

    @Query("SELECT * FROM transcriptions WHERE text LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    fun searchByKeyword(keyword: String): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<Transcription>
}
