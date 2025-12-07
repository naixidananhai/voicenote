package com.voicelife.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.voicelife.assistant.service.VoiceMonitorService

/**
 * 服务重启接收器
 * 用于AlarmManager定期检查和重启服务
 */
class ServiceRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ServiceRestartReceiver"
        const val ACTION_RESTART_SERVICE = "com.voicelife.assistant.ACTION_RESTART_SERVICE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received restart broadcast")

        when (intent.action) {
            ACTION_RESTART_SERVICE -> {
                // 检查服务是否应该运行
                val prefs = context.getSharedPreferences("voice_assistant_prefs", Context.MODE_PRIVATE)
                val shouldRun = prefs.getBoolean("service_was_running", false)

                if (shouldRun) {
                    // 检查服务是否正在运行
                    if (!isServiceRunning(context)) {
                        Log.d(TAG, "Service not running, restarting...")
                        startService(context)
                    } else {
                        Log.d(TAG, "Service already running")
                    }
                }
            }
        }
    }

    /**
     * 启动服务
     */
    private fun startService(context: Context) {
        try {
            val serviceIntent = Intent(context, VoiceMonitorService::class.java)
            VoiceMonitorService.startService(serviceIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d(TAG, "Service restart command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart service", e)
        }
    }

    /**
     * 检查服务是否正在运行
     */
    private fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (VoiceMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
