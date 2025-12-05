package com.voicelife.assistant.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val storageManager: StorageManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * 加载数据
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // 加载录音统计
                val stats = recordingRepository.getStatistics()

                // 加载存储信息
                val storage = storageManager.getStorageInfo()

                // 检查权限
                val hasPermissions = permissionManager.hasAllRequiredPermissions()
                val missingPermissions = permissionManager.getMissingPermissions()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statistics = stats,
                    storageInfo = storage,
                    hasAllPermissions = hasPermissions,
                    missingPermissions = missingPermissions
                )
            } catch (e: Exception) {
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
        if (!permissionManager.hasAllRequiredPermissions()) {
            _uiState.value = _uiState.value.copy(
                showPermissionDialog = true
            )
            return
        }

        val intent = Intent(getApplication(), VoiceMonitorService::class.java)
        VoiceMonitorService.startService(intent)
        getApplication<Application>().startForegroundService(intent)

        _uiState.value = _uiState.value.copy(isServiceRunning = true)
    }

    /**
     * 停止服务
     */
    fun stopService() {
        val intent = Intent(getApplication(), VoiceMonitorService::class.java)
        VoiceMonitorService.stopService(intent)
        getApplication<Application>().startService(intent)

        _uiState.value = _uiState.value.copy(isServiceRunning = false)
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
    val cleanupMessage: String? = null
)
