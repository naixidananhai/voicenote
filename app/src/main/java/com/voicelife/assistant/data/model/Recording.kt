package com.voicelife.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val duration: Int,  // 秒
    val fileSize: Long,  // 字节
    val createdAt: Long,  // Unix timestamp
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val transcribedAt: Long? = null,
    val deleteAt: Long  // 7天后删除
)

enum class TranscriptionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
