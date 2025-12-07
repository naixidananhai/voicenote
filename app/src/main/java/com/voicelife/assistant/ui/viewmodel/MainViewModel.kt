package com.voicelife.assistant.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicelife.assistant.data.repository.RecordingRepository
import com.voicelife.assistant.data.repository.RecordingStatistics
import com.voicelife.assistant.service.VoiceMonitorService
import com.voicelife.assistant.storage.StorageInfo
import com.voicelife.assistant.storage.StorageManager
import com.voicelife.assistant.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 主界面ViewModel
 * 管理服务状态、录音统计、存储信息等
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val permissionManager: PermissionManager,
    private val recordingRepository: RecordingRepository,
    private val storageManager: StorageManager,
    private val debugLogger: com.voicelife.assistant.utils.DebugLogger
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<com.voicelife.assistant.utils.LogEntry>> = debugLogger.logs

    init {
        debugLogger.i("MainViewModel", "应用启动")
        loadData()
    }

    /**
     * 加载数据
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                debugLogger.i("MainViewModel", "开始加载数据...")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // 加载录音统计
                val stats = recordingRepository.getStatistics()
                debugLogger.d("MainViewModel", "录音统计: ${stats.totalCount}条")

                // 加载存储信息
                val storage = storageManager.getStorageInfo()
                debugLogger.d("MainViewModel", "存储空间: ${storage.availableSpaceMB}MB可用")

                // 检查权限
                val hasPermissions = permissionManager.hasAllRequiredPermissions()
                val missingPermissions = permissionManager.getMissingPermissions()
                debugLogger.i("MainViewModel", "权限检查: ${if (hasPermissions) "已授予" else "缺少${missingPermissions.size}个权限"}")

                // 检查服务是否真的在运行
                val isServiceRunning = isServiceRunning()
                debugLogger.d("MainViewModel", "服务状态: ${if (isServiceRunning) "运行中" else "已停止"}")

                // 获取录音文件路径
                val recordingsPath = storageManager.getRecordingsDir().absolutePath

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statistics = stats,
                    storageInfo = storage,
                    hasAllPermissions = hasPermissions,
                    missingPermissions = missingPermissions,
                    recordingsPath = recordingsPath,
                    isServiceRunning = isServiceRunning
                )
            } catch (e: Exception) {
                debugLogger.e("MainViewModel", "加载数据失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * 启动服务
     */
    fun startService() {
        debugLogger.i("MainViewModel", "尝试启动服务...")
        if (!permissionManager.hasAllRequiredPermissions()) {
            debugLogger.w("MainViewModel", "权限不足，无法启动服务")
            _uiState.value = _uiState.value.copy(
                showPermissionDialog = true
            )
            return
        }

        val intent = Intent(getApplication(), VoiceMonitorService::class.java)
        VoiceMonitorService.startService(intent)
        getApplication<Application>().startForegroundService(intent)
        debugLogger.i("MainViewModel", "服务启动命令已发送")

        _uiState.value = _uiState.value.copy(isServiceRunning = true)
    }

    /**
     * 停止服务
     */
    fun stopService() {
        debugLogger.i("MainViewModel", "停止服务...")
        val intent = Intent(getApplication(), VoiceMonitorService::class.java)
        VoiceMonitorService.stopService(intent)
        getApplication<Application>().startService(intent)
        debugLogger.i("MainViewModel", "服务停止命令已发送")

        _uiState.value = _uiState.value.copy(isServiceRunning = false)
    }

    /**
     * 清空日志
     */
    fun clearLogs() {
        debugLogger.clear()
        debugLogger.i("MainViewModel", "日志已清空")
    }

    /**
     * 复制全部日志到剪贴板
     */
    fun copyAllLogs() {
        viewModelScope.launch {
            try {
                val currentLogs = logs.value
                if (currentLogs.isEmpty()) {
                    debugLogger.w("MainViewModel", "没有日志可复制")
                    return@launch
                }

                // 格式化日志文本
                val logText = buildString {
                    appendLine("=== VoiceLife 语音助手日志 ===")
                    appendLine("总计: ${currentLogs.size} 条")
                    appendLine("时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine()
                    
                    currentLogs.reversed().forEach { log ->
                        val levelStr = when (log.level) {
                            com.voicelife.assistant.utils.LogLevel.DEBUG -> "DEBUG"
                            com.voicelife.assistant.utils.LogLevel.INFO -> "INFO "
                            com.voicelife.assistant.utils.LogLevel.WARN -> "WARN "
                            com.voicelife.assistant.utils.LogLevel.ERROR -> "ERROR"
                        }
                        appendLine("${log.timestamp} [$levelStr] [${log.tag}] ${log.message}")
                    }
                }

                // 复制到剪贴板
                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("VoiceLife Logs", logText)
                clipboard.setPrimaryClip(clip)

                debugLogger.i("MainViewModel", "✅ 已复制 ${currentLogs.size} 条日志到剪贴板")
            } catch (e: Exception) {
                debugLogger.e("MainViewModel", "复制日志失败: ${e.message}")
            }
        }
    }

    /**
     * 打开录音文件夹
     */
    fun openRecordingsFolder() {
        viewModelScope.launch {
            try {
                val recordingsDir = storageManager.getRecordingsDir()
                debugLogger.i("MainViewModel", "📁 录音文件夹:")
                debugLogger.i("MainViewModel", recordingsDir.absolutePath)
                
                // 列出所有录音文件
                val pendingFiles = File(recordingsDir, "pending").listFiles()?.toList() ?: emptyList()
                val processingFiles = File(recordingsDir, "processing").listFiles()?.toList() ?: emptyList()
                val completedFiles = File(recordingsDir, "completed").listFiles()?.toList() ?: emptyList()
                val failedFiles = File(recordingsDir, "failed").listFiles()?.toList() ?: emptyList()
                
                debugLogger.i("MainViewModel", "📊 文件统计:")
                debugLogger.i("MainViewModel", "  待处理: ${pendingFiles.size}个")
                debugLogger.i("MainViewModel", "  处理中: ${processingFiles.size}个")
                debugLogger.i("MainViewModel", "  已完成: ${completedFiles.size}个")
                debugLogger.i("MainViewModel", "  失败: ${failedFiles.size}个")
                
                // 列出最近的5个文件
                val allFiles = (pendingFiles + processingFiles + completedFiles + failedFiles)
                    .sortedByDescending { it.lastModified() }
                
                if (allFiles.isNotEmpty()) {
                    debugLogger.i("MainViewModel", "📝 最近的文件:")
                    allFiles.take(5).forEach { file ->
                        val sizeKB = file.length() / 1024
                        val folder = file.parentFile?.name ?: ""
                        debugLogger.d("MainViewModel", "  [$folder] ${file.name} (${sizeKB}KB)")
                    }
                    
                    // 显示adb命令
                    debugLogger.i("MainViewModel", "💻 使用adb获取文件:")
                    debugLogger.i("MainViewModel", "adb pull ${recordingsDir.absolutePath} .")
                } else {
                    debugLogger.w("MainViewModel", "⚠️ 文件夹为空，还没有录音")
                }
                
                // 打开文件管理器
                withContext(Dispatchers.Main) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                android.net.Uri.parse(recordingsDir.absolutePath),
                                "resource/folder"
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        // 尝试打开文件管理器
                        val app = getApplication<Application>()
                        if (intent.resolveActivity(app.packageManager) != null) {
                            app.startActivity(intent)
                            debugLogger.i("MainViewModel", "✅ 已打开文件管理器")
                        } else {
                            // 备用方案：使用DocumentsUI
                            val documentsIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            app.startActivity(documentsIntent)
                            debugLogger.i("MainViewModel", "✅ 已打开文档选择器")
                        }
                    } catch (e: Exception) {
                        debugLogger.w("MainViewModel", "⚠️ 无法打开文件管理器: ${e.message}")
                        debugLogger.i("MainViewModel", "💡 请手动打开: 文件管理器 > Download > VoiceAssistant")
                    }
                }
                
            } catch (e: Exception) {
                debugLogger.e("MainViewModel", "打开文件夹失败: ${e.message}")
            }
        }
    }

    /**
     * 切换服务状态
     */
    fun toggleService() {
        if (_uiState.value.isServiceRunning) {
            stopService()
        } else {
            startService()
        }
    }

    /**
     * 检查服务是否正在运行
     */
    private fun isServiceRunning(): Boolean {
        val manager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (VoiceMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * 检查并重启服务
     * 如果服务应该运行但实际没有运行，则自动重启
     */
    fun checkAndRestartService() {
        viewModelScope.launch {
            try {
                // 检查服务是否应该运行
                val prefs = getApplication<Application>().getSharedPreferences("voice_assistant_prefs", Context.MODE_PRIVATE)
                val shouldRun = prefs.getBoolean("service_was_running", false)
                
                // 检查服务是否真的在运行
                val isRunning = isServiceRunning()
                
                debugLogger.d("MainViewModel", "服务检查: 应该运行=$shouldRun, 实际运行=$isRunning")
                
                if (shouldRun && !isRunning) {
                    // 服务应该运行但没有运行，自动重启
                    debugLogger.w("MainViewModel", "⚠️ 检测到服务异常停止，自动重启...")
                    startService()
                }
            } catch (e: Exception) {
                debugLogger.e("MainViewModel", "检查服务失败: ${e.message}")
            }
        }
    }

    /**
     * 清理所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                storageManager.clearAllData()
                loadData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 执行清理
     */
    fun performCleanup() {
        viewModelScope.launch {
            try {
                val result = storageManager.performCleanup()
                loadData()

                _uiState.value = _uiState.value.copy(
                    cleanupMessage = "已清理 ${result.deletedFiles} 个文件，释放 ${result.getFreedSpaceMB()} MB"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 关闭权限对话框
     */
    fun dismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = false)
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 清除清理消息
     */
    fun clearCleanupMessage() {
        _uiState.value = _uiState.value.copy(cleanupMessage = null)
    }
}

/**
 * 主界面UI状态
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val isServiceRunning: Boolean = false,
    val statistics: RecordingStatistics? = null,
    val storageInfo: StorageInfo? = null,
    val hasAllPermissions: Boolean = false,
    val missingPermissions: List<com.voicelife.assistant.utils.PermissionInfo> = emptyList(),
    val showPermissionDialog: Boolean = false,
    val error: String? = null,
    val cleanupMessage: String? = null,
    val recordingsPath: String? = null
)
