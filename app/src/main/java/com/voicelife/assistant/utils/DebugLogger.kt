package com.voicelife.assistant.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调试日志管理器
 * 收集应用运行日志并显示在UI上
 */
@Singleton
class DebugLogger @Inject constructor() {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val maxLogs = 100  // 最多保留100条日志
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 记录日志
     */
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(
            timestamp = timestamp,
            tag = tag,
            message = message,
            level = level
        )

        // 同时输出到Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }

        // 添加到UI日志列表
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry)  // 新日志在最前面
        if (currentLogs.size > maxLogs) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.value = currentLogs
    }

    fun d(tag: String, message: String) = log(tag, message, LogLevel.DEBUG)
    fun i(tag: String, message: String) = log(tag, message, LogLevel.INFO)
    fun w(tag: String, message: String) = log(tag, message, LogLevel.WARN)
    fun e(tag: String, message: String) = log(tag, message, LogLevel.ERROR)

    /**
     * 清空日志
     */
    fun clear() {
        _logs.value = emptyList()
    }
}

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: LogLevel
)

/**
 * 日志级别
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}
