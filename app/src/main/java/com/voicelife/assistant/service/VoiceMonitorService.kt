package com.voicelife.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class VoiceMonitorService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // 服务初始化
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
    }
}
