package com.voicelife.assistant.di

import android.content.Context
import androidx.room.Room
import com.voicelife.assistant.data.database.AppDatabase
import com.voicelife.assistant.data.database.RecordingDao
import com.voicelife.assistant.data.database.TranscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_assistant_db"
        ).build()
    }

    @Provides
    fun provideRecordingDao(database: AppDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    fun provideTranscriptionDao(database: AppDatabase): TranscriptionDao {
        return database.transcriptionDao()
    }
}
