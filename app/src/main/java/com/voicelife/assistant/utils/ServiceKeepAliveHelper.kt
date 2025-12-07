package com.voicelife.assistant.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.voicelife.assistant.receiver.ServiceRestartReceiver

/**
 * 服务保活助手
 * 使用AlarmManager定期检查服务状态
 */
object ServiceKeepAliveHelper {
    private const val TAG = "ServiceKeepAliveHelper"
    private const val CHECK_INTERVAL = 2 * 60 * 1000L  // 2分钟检查一次

    /**
     * 启动定期检查
     */
    fun startKeepAlive(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ServiceRestartReceiver::class.java).apply {
                action = ServiceRestartReceiver.ACTION_RESTART_SERVICE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            // 使用setRepeating确保定期执行
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + CHECK_INTERVAL,
                CHECK_INTERVAL,
                pendingIntent
            )

            Log.d(TAG, "Keep-alive alarm started (interval: ${CHECK_INTERVAL / 1000}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start keep-alive alarm", e)
        }
    }

    /**
     * 停止定期检查
     */
    fun stopKeepAlive(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ServiceRestartReceiver::class.java).apply {
                action = ServiceRestartReceiver.ACTION_RESTART_SERVICE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Keep-alive alarm stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop keep-alive alarm", e)
        }
    }
}
