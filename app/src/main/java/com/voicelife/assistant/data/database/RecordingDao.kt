package com.voicelife.assistant.data.database

import androidx.room.*
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): Recording?

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: TranscriptionStatus): List<Recording>

    @Query("SELECT * FROM recordings WHERE deleteAt < :timestamp")
    suspend fun getExpiredRecordings(timestamp: Long): List<Recording>

    @Query("DELETE FROM recordings WHERE deleteAt < :timestamp")
    suspend fun deleteExpired(timestamp: Long): Int
}
