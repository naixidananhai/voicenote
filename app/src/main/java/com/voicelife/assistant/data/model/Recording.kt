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
    val transcriptionText: String? = null,  // 转录文本
    val transcribedAt: Long? = null,
    val deleteAt: Long  // 7天后删除
)

enum class TranscriptionStatus {
    PENDING,      // 待转录
    PROCESSING,   // 转录中
    COMPLETED,    // 已完成
    FAILED        // 转录失败
}
