package com.voicelife.assistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.model.Transcription

@Database(
    entities = [Recording::class, Transcription::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun transcriptionDao(): TranscriptionDao
}
