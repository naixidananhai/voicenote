package com.voicelife.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.voicelife.assistant.service.VoiceMonitorService

/**
 * 系统启动和服务重启接收器
 * 
 * 监听以下事件：
 * 1. 系统启动完成 (BOOT_COMPLETED)
 * 2. 应用更新完成 (MY_PACKAGE_REPLACED)
 * 3. 用户解锁 (USER_PRESENT)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device boot completed")
                // 检查服务之前是否在运行
                if (wasServiceRunning(context)) {
                    startService(context)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App updated")
                // 应用更新后，如果之前在运行则重启
                if (wasServiceRunning(context)) {
                    startService(context)
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User unlocked device")
                // 用户解锁后，检查服务是否需要重启
                if (wasServiceRunning(context) && !isServiceRunning(context)) {
                    startService(context)
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
            
            Log.d(TAG, "Service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
        }
    }

    /**
     * 检查服务之前是否在运行
     */
    private fun wasServiceRunning(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICE_WAS_RUNNING, false)
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

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "voice_assistant_prefs"
        private const val KEY_SERVICE_WAS_RUNNING = "service_was_running"
        
        /**
         * 保存服务运行状态
         */
        fun saveServiceState(context: Context, isRunning: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SERVICE_WAS_RUNNING, isRunning).apply()
            Log.d(TAG, "Service state saved: $isRunning")
        }
    }
}
