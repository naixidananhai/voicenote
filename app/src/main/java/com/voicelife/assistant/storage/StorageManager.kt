package com.voicelife.assistant.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.voicelife.assistant.data.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存储管理器
 * 负责监控存储空间、自动清理过期文件
 *
 * 功能:
 * 1. 监控可用存储空间
 * 2. 自动清理过期录音(7天)
 * 3. 清理临时文件和失败文件
 * 4. 提供存储统计信息
 *
 * 空间策略:
 * - 警戒线: 500MB
 * - 低于500MB时触发清理
 */
@Singleton
class StorageManager @Inject constructor(
    private val context: Context,
    private val recordingRepository: RecordingRepository
) {
    // 使用公共Download目录，方便用户访问
    private val recordingsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "VoiceAssistant"
    )

    companion object {
        private const val TAG = "StorageManager"
        private const val MIN_AVAILABLE_SPACE_MB = 500L  // 500MB
        private const val BYTES_PER_MB = 1024L * 1024L
    }

    /**
     * 初始化存储目录
     */
    fun init() {
        recordingsDir.mkdirs()
        File(recordingsDir, "pending").mkdirs()
        File(recordingsDir, "processing").mkdirs()
        File(recordingsDir, "completed").mkdirs()
        File(recordingsDir, "failed").mkdirs()

        Log.d(TAG, "Storage manager initialized: ${recordingsDir.absolutePath}")
    }

    /**
     * 获取可用空间(MB)
     */
    fun getAvailableSpaceMB(): Long {
        return try {
            val stat = StatFs(recordingsDir.absolutePath)
            val availableBytes = stat.availableBytes
            availableBytes / BYTES_PER_MB
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available space", e)
            0L
        }
    }

    /**
     * 获取已使用空间(MB)
     */
    fun getUsedSpaceMB(): Long {
        return try {
            calculateDirectorySize(recordingsDir) / BYTES_PER_MB
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get used space", e)
            0L
        }
    }

    /**
     * 检查存储空间是否足够
     */
    fun hasEnoughSpace(): Boolean {
        val available = getAvailableSpaceMB()
        return available >= MIN_AVAILABLE_SPACE_MB
    }

    /**
     * 执行清理
     * @return 清理的文件数
     */
    suspend fun performCleanup(): CleanupResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting cleanup...")

        var deletedFiles = 0
        var freedSpaceBytes = 0L

        try {
            // 1. 清理过期录音
            val expiredCount = recordingRepository.deleteExpiredRecordings()
            deletedFiles += expiredCount
            Log.d(TAG, "Deleted $expiredCount expired recordings")

            // 2. 清理失败的文件
            val failedDir = File(recordingsDir, "failed")
            val failedFiles = failedDir.listFiles() ?: emptyArray()
            for (file in failedFiles) {
                if (file.isFile) {
                    freedSpaceBytes += file.length()
                    file.delete()
                    deletedFiles++
                }
            }
            Log.d(TAG, "Deleted ${failedFiles.size} failed files")

            // 3. 清理孤立的音频文件(数据库中不存在的)
            val orphanCount = cleanOrphanFiles()
            deletedFiles += orphanCount
            Log.d(TAG, "Deleted $orphanCount orphan files")

            Log.d(TAG, "Cleanup completed: $deletedFiles files, ${freedSpaceBytes / BYTES_PER_MB}MB freed")

        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }

        CleanupResult(deletedFiles, freedSpaceBytes)
    }

    /**
     * 清理孤立文件
     * (文件存在但数据库中没有记录)
     */
    private suspend fun cleanOrphanFiles(): Int {
        var count = 0

        val dirs = arrayOf("pending", "processing", "completed")
        for (dirName in dirs) {
            val dir = File(recordingsDir, dirName)
            val files = dir.listFiles() ?: continue

            for (file in files) {
                if (!file.isFile) continue

                // 检查数据库中是否存在
                val recordings = recordingRepository.getAllRecordings()
                // TODO: 这里需要优化,应该直接查询数据库而不是Flow
                // 暂时简化处理,只删除明显的临时文件
                if (file.name.endsWith(".tmp")) {
                    file.delete()
                    count++
                }
            }
        }

        return count
    }

    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L

        val files = directory.listFiles() ?: return 0L

        for (file in files) {
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }

        return size
    }

    /**
     * 获取存储统计信息
     */
    suspend fun getStorageInfo(): StorageInfo {
        val availableMB = getAvailableSpaceMB()
        val usedMB = getUsedSpaceMB()
        val stats = recordingRepository.getStatistics()

        return StorageInfo(
            availableSpaceMB = availableMB,
            usedSpaceMB = usedMB,
            totalRecordings = stats.totalCount,
            pendingRecordings = stats.pendingCount,
            hasEnoughSpace = availableMB >= MIN_AVAILABLE_SPACE_MB
        )
    }

    /**
     * 清理所有数据(用于重置应用)
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除所有文件
            recordingsDir.deleteRecursively()

            // 重新创建目录
            init()

            Log.d(TAG, "All data cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data", e)
            false
        }
    }

    /**
     * 获取录音目录
     */
    fun getRecordingsDir(): File = recordingsDir
}

/**
 * 清理结果
 */
data class CleanupResult(
    val deletedFiles: Int,
    val freedSpaceBytes: Long
) {
    fun getFreedSpaceMB(): Long = freedSpaceBytes / (1024L * 1024L)
}

/**
 * 存储信息
 */
data class StorageInfo(
    val availableSpaceMB: Long,
    val usedSpaceMB: Long,
    val totalRecordings: Int,
    val pendingRecordings: Int,
    val hasEnoughSpace: Boolean
)
