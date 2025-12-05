package com.voicelife.assistant.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voicelife.assistant.R
import com.voicelife.assistant.ui.main.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理助手
 * 负责创建和管理前台服务通知
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "voice_monitor_service"
        private const val CHANNEL_NAME = "语音监听服务"
        private const val NOTIFICATION_ID = 1001

        // 通知优先级
        private const val IMPORTANCE = NotificationManager.IMPORTANCE_LOW
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                IMPORTANCE
            ).apply {
                description = "显示语音监听服务的运行状态"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     * @param state 服务状态
     */
    fun createServiceNotification(state: ServiceState): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = when (state) {
            is ServiceState.Idle -> "等待人声..."
            is ServiceState.Recording -> "正在录音 ${formatDuration(state.durationSeconds)}"
            is ServiceState.Processing -> "处理中 (队列: ${state.queueSize})"
            is ServiceState.Error -> "错误: ${state.message}"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("语音助手正在监听")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)  // TODO: 替换为自定义图标
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * 更新通知
     */
    fun updateNotification(state: ServiceState) {
        val notification = createServiceNotification(state)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 创建警告通知
     */
    fun showWarningNotification(type: WarningType) {
        val (title, message) = when (type) {
            WarningType.STORAGE_LOW -> Pair("存储空间不足", "可用空间低于500MB,请清理文件")
            WarningType.PERMISSION_LOST -> Pair("权限丢失", "录音权限已被撤销,请重新授权")
            WarningType.TRANSCRIPTION_FAILED -> Pair("转换失败", "部分录音转换失败,请检查")
            WarningType.MICROPHONE_BUSY -> Pair("麦克风被占用", "其他应用正在使用麦克风")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(type.ordinal + 2000, notification)
    }

    /**
     * 格式化时长显示
     */
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    /**
     * 获取通知ID
     */
    fun getNotificationId(): Int = NOTIFICATION_ID
}

/**
 * 服务状态
 */
sealed class ServiceState {
    object Idle : ServiceState()

    data class Recording(
        val durationSeconds: Int
    ) : ServiceState()

    data class Processing(
        val queueSize: Int
    ) : ServiceState()

    data class Error(
        val message: String
    ) : ServiceState()
}

/**
 * 警告类型
 */
enum class WarningType {
    STORAGE_LOW,
    PERMISSION_LOST,
    TRANSCRIPTION_FAILED,
    MICROPHONE_BUSY
}
