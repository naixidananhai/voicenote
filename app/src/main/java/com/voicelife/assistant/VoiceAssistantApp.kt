package com.voicelife.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VoiceAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化日志系统
    }
}
