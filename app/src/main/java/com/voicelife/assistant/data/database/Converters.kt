package com.voicelife.assistant.data.database

import androidx.room.TypeConverter
import com.voicelife.assistant.data.model.TranscriptionStatus

class Converters {
    @TypeConverter
    fun fromTranscriptionStatus(value: TranscriptionStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTranscriptionStatus(value: String): TranscriptionStatus {
        return TranscriptionStatus.valueOf(value)
    }
}
