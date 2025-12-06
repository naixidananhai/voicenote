package com.voicelife.assistant.ui.main

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicelife.assistant.ui.viewmodel.MainViewModel
import com.voicelife.assistant.utils.LogLevel
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
    val logs by viewModel.logs.collectAsState()

    // 整个页面可滚动
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
                totalSize = stats.getTotalSizeMB(),
                onRefresh = { viewModel.loadData() }
            )
        }

        // 存储信息卡片
        uiState.storageInfo?.let { storage ->
            StorageCard(
                availableMB = storage.availableSpaceMB,
                usedMB = storage.usedSpaceMB,
                hasEnoughSpace = storage.hasEnoughSpace,
                recordingsPath = uiState.recordingsPath,
                onCleanup = { viewModel.performCleanup() },
                onOpenFolder = { viewModel.openRecordingsFolder() }
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

        // 实时日志面板（固定高度400dp）
        DebugLogCard(
            logs = logs,
            onClear = { viewModel.clearLogs() },
            onCopyAll = { viewModel.copyAllLogs() },
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )

        // 加载指示器
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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
    totalSize: Long,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "录音统计",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onRefresh) {
                    Text("🔄 刷新")
                }
            }

            StatRow("总录音数", "$totalCount 段")
            StatRow("总时长", "$totalDuration 分钟")
            StatRow("待处理", "$pendingCount 个")
            StatRow("占用空间", "$totalSize MB")
            
            if (totalCount == 0) {
                Text(
                    text = "💡 提示：开始监听并说话后，录音会自动保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
    recordingsPath: String?,
    onCleanup: () -> Unit,
    onOpenFolder: () -> Unit
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

            // 显示录音文件路径
            recordingsPath?.let { path ->
                Text(
                    text = "📁 $path",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (!hasEnoughSpace) {
                Text(
                    text = "⚠️ 存储空间不足500MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenFolder,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("打开文件夹")
                }
                
                Button(
                    onClick = onCleanup,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清理过期")
                }
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

/**
 * 调试日志卡片
 */
@Composable
fun DebugLogCard(
    logs: List<com.voicelife.assistant.utils.LogEntry>,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍 实时日志 (${logs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onCopyAll) {
                        Text("📋 复制", color = Color(0xFF81C784))
                    }
                    TextButton(onClick = onClear) {
                        Text("清空", color = Color(0xFF64B5F6))
                    }
                }
            }

            Divider(color = Color(0xFF424242))

            // 日志列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                reverseLayout = false
            ) {
                items(logs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

/**
 * 单条日志
 */
@Composable
fun LogItem(log: com.voicelife.assistant.utils.LogEntry) {
    val color = when (log.level) {
        LogLevel.DEBUG -> Color(0xFF9E9E9E)
        LogLevel.INFO -> Color(0xFF64B5F6)
        LogLevel.WARN -> Color(0xFFFFB74D)
        LogLevel.ERROR -> Color(0xFFE57373)
    }

    val icon = when (log.level) {
        LogLevel.DEBUG -> "🔹"
        LogLevel.INFO -> "ℹ️"
        LogLevel.WARN -> "⚠️"
        LogLevel.ERROR -> "❌"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "${log.timestamp} $icon [${log.tag}] ${log.message}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            lineHeight = 14.sp
        )
    }
}
