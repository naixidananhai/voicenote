package com.voicelife.assistant.ui.recordings

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.ui.viewmodel.RecordingsViewModel
import com.voicelife.assistant.ui.viewmodel.QuickDateFilter
import com.voicelife.assistant.ui.viewmodel.TimeSlotFilter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(viewModel: RecordingsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimeSlotMenu by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录音列表") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { viewModel.loadRecordings() }) { Icon(Icons.Default.Refresh, "刷新") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 简洁的筛选按钮栏
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 日期选择按钮
                    FilterChip(
                        selected = uiState.filterCriteria.quickFilter != QuickDateFilter.ALL,
                        onClick = { showDatePicker = true },
                        label = { 
                            Text(if (uiState.filterCriteria.quickFilter == QuickDateFilter.ALL) "选择日期" 
                                 else uiState.filterCriteria.quickFilter.displayName) 
                        },
                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (uiState.filterCriteria.quickFilter != QuickDateFilter.ALL) {
                            { Icon(Icons.Default.Close, "清除", modifier = Modifier.size(16.dp).clickable { viewModel.setQuickDateFilter(QuickDateFilter.ALL) }) }
                        } else null
                    )
                    // 时段选择按钮
                    Box {
                        FilterChip(
                            selected = uiState.filterCriteria.timeSlot != TimeSlotFilter.ALL,
                            onClick = { showTimeSlotMenu = true },
                            label = { 
                                Text(if (uiState.filterCriteria.timeSlot == TimeSlotFilter.ALL) "选择时段" 
                                     else uiState.filterCriteria.timeSlot.displayName) 
                            },
                            leadingIcon = { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (uiState.filterCriteria.timeSlot != TimeSlotFilter.ALL) {
                                { Icon(Icons.Default.Close, "清除", modifier = Modifier.size(16.dp).clickable { viewModel.setTimeSlotFilter(TimeSlotFilter.ALL) }) }
                            } else null
                        )
                        DropdownMenu(expanded = showTimeSlotMenu, onDismissRequest = { showTimeSlotMenu = false }) {
                            TimeSlotFilter.values().forEach { slot ->
                                DropdownMenuItem(
                                    text = { Text(slot.displayName) },
                                    onClick = { viewModel.setTimeSlotFilter(slot); showTimeSlotMenu = false },
                                    leadingIcon = if (uiState.filterCriteria.timeSlot == slot) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
                when {
                    uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    uiState.filteredRecordings.isEmpty() -> EmptyState(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center), hasFilter = uiState.filterCriteria.hasFilter())
                    else -> RecordingsList(uiState.filteredRecordings, uiState.playingRecordingId, uiState.isPlaying, uiState.transcribingRecordingId,
                        { viewModel.playRecording(it) }, { viewModel.deleteRecording(it) }, { viewModel.transcribeRecording(it) })
                }
            }
            uiState.error?.let { error ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("确定") } }) { Text(error) }
            }
        }
    }
    // 日期选择器
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setCustomDate(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }
}




@Composable
fun RecordingsList(recordings: List<Recording>, playingRecordingId: Long?, isPlaying: Boolean, transcribingRecordingId: Long?,
    onPlayClick: (Recording) -> Unit, onDeleteClick: (Recording) -> Unit, onTranscribeClick: (Recording) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(recordings) { recording ->
            RecordingItem(recording, playingRecordingId == recording.id && isPlaying, transcribingRecordingId == recording.id,
                { onPlayClick(recording) }, { onDeleteClick(recording) }, { onTranscribeClick(recording) })
        }
    }
}

@Composable
fun RecordingItem(recording: Recording, isPlaying: Boolean, isTranscribing: Boolean, onPlayClick: () -> Unit, onDeleteClick: () -> Unit, onTranscribeClick: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hasTranscription = recording.transcriptionText != null
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatTimestampFriendly(recording.createdAt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp)) {
                    if (isPlaying) PulsingPlayIcon() else Icon(Icons.Default.PlayArrow, "播放", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (hasTranscription) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text(text = recording.transcriptionText!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp))
                }
            } else if (isTranscribing) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp)); Text("正在转录中...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium) {
                    Text("点击「转文字」按钮进行语音识别", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${formatDuration(recording.duration)} · ${formatFileSize(recording.fileSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(java.io.File(recording.filePath).name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onTranscribeClick, enabled = !isTranscribing) {
                        if (isTranscribing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(if (hasTranscription) "重新转" else "转文字", color = if (hasTranscription) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("删除录音") }, text = { Text("确定要删除这条录音吗？此操作无法撤销。") },
            confirmButton = { TextButton(onClick = { onDeleteClick(); showDeleteDialog = false }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } })
    }
}

@Composable
fun PulsingPlayIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "scale")
    Icon(Icons.Default.Close, "暂停", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((28 * scale).dp))
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, hasFilter: Boolean = false) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(if (hasFilter) Icons.Default.Search else Icons.Default.Info, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (hasFilter) "没有符合条件的录音" else "还没有录音", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(if (hasFilter) "尝试调整筛选条件" else "开始聆听后，检测到的人声会自动保存在这里", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatTimestampFriendly(timestamp: Long): String {
    val date = Date(timestamp)
    val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val df = SimpleDateFormat("MM月dd日", Locale.getDefault())
    val ff = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000
    val diff = System.currentTimeMillis() - timestamp
    return when {
        timestamp >= todayStart -> "今天 ${tf.format(date)}"
        timestamp >= yesterdayStart -> "昨天 ${tf.format(date)}"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${df.format(date)} ${tf.format(date)}"
        else -> ff.format(date)
    }
}
