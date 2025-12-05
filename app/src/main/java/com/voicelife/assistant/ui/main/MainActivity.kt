package com.voicelife.assistant.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voicelife.assistant.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主Activity
 * 显示服务控制、统计信息、权限管理
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // 权限请求启动器
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    /**
     * 请求权限
     */
    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}

/**
 * 主界面
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "VoiceLife 语音助手",
            style = MaterialTheme.typography.headlineMedium
        )

        // 权限状态卡片
        if (!uiState.hasAllPermissions) {
            PermissionCard(
                missingPermissions = uiState.missingPermissions,
                onRequestPermissions = onRequestPermissions
            )
        }

        // 服务控制卡片
        ServiceControlCard(
            isRunning = uiState.isServiceRunning,
            hasPermissions = uiState.hasAllPermissions,
            onToggleService = { viewModel.toggleService() }
        )

        // 统计信息卡片
        uiState.statistics?.let { stats ->
            StatisticsCard(
                totalCount = stats.totalCount,
                totalDuration = stats.getTotalDurationMinutes(),
                pendingCount = stats.pendingCount,
                totalSize = stats.getTotalSizeMB()
            )
        }

        // 存储信息卡片
        uiState.storageInfo?.let { storage ->
            StorageCard(
                availableMB = storage.availableSpaceMB,
                usedMB = storage.usedMB,
                hasEnoughSpace = storage.hasEnoughSpace,
                onCleanup = { viewModel.performCleanup() }
            )
        }

        // 清理消息
        uiState.cleanupMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearCleanupMessage() }) {
                        Text("确定")
                    }
                }
            ) {
                Text(message)
            }
        }

        // 错误消息
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("确定")
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // 加载指示器
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * 权限卡片
 */
@Composable
fun PermissionCard(
    missingPermissions: List<com.voicelife.assistant.utils.PermissionInfo>,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠️ 需要权限",
                style = MaterialTheme.typography.titleMedium
            )

            missingPermissions.forEach { permission ->
                Text(
                    text = "• ${permission.name}: ${permission.description}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("授予权限")
            }
        }
    }
}

/**
 * 服务控制卡片
 */
@Composable
fun ServiceControlCard(
    isRunning: Boolean,
    hasPermissions: Boolean,
    onToggleService: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "服务控制",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRunning) "服务运行中" else "服务已停止",
                    style = MaterialTheme.typography.bodyLarge
                )

                // 状态指示器
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(12.dp)
                ) {}
            }

            Button(
                onClick = onToggleService,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasPermissions
            ) {
                Text(if (isRunning) "停止监听" else "开始监听")
            }

            if (!hasPermissions) {
                Text(
                    text = "请先授予所需权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 统计信息卡片
 */
@Composable
fun StatisticsCard(
    totalCount: Int,
    totalDuration: Int,
    pendingCount: Int,
    totalSize: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "录音统计",
                style = MaterialTheme.typography.titleMedium
            )

            StatRow("总录音数", "$totalCount 段")
            StatRow("总时长", "$totalDuration 分钟")
            StatRow("待处理", "$pendingCount 个")
            StatRow("占用空间", "$totalSize MB")
        }
    }
}

/**
 * 存储卡片
 */
@Composable
fun StorageCard(
    availableMB: Long,
    usedMB: Long,
    hasEnoughSpace: Boolean,
    onCleanup: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "存储管理",
                style = MaterialTheme.typography.titleMedium
            )

            StatRow("可用空间", "$availableMB MB")
            StatRow("已使用", "$usedMB MB")

            if (!hasEnoughSpace) {
                Text(
                    text = "⚠️ 存储空间不足500MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onCleanup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清理过期文件")
            }
        }
    }
}

/**
 * 统计行
 */
@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
