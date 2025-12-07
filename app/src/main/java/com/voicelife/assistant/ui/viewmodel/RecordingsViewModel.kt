package com.voicelife.assistant.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.repository.RecordingRepository
import com.voicelife.assistant.utils.AudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * 快捷日期筛选选项
 */
enum class QuickDateFilter(val displayName: String) {
    ALL("全部"),
    TODAY("今天"),
    YESTERDAY("昨天"),
    LAST_7_DAYS("最近7天"),
    LAST_30_DAYS("最近30天"),
    CUSTOM("自定义日期")
}

/**
 * 时段筛选选项
 */
enum class TimeSlotFilter(val displayName: String, val startHour: Int, val endHour: Int) {
    ALL("全天", 0, 24),
    NIGHT("凌晨 (0:00-6:00)", 0, 6),
    MORNING("上午 (6:00-12:00)", 6, 12),
    AFTERNOON("下午 (12:00-18:00)", 12, 18),
    EVENING("晚上 (18:00-24:00)", 18, 24)
}

/**
 * 筛选条件
 */
data class FilterCriteria(
    val quickFilter: QuickDateFilter = QuickDateFilter.ALL,
    val customDate: Long? = null,  // 自定义日期的时间戳（当天0点）
    val timeSlot: TimeSlotFilter = TimeSlotFilter.ALL
) {
    fun getDisplayText(): String {
        return when {
            quickFilter == QuickDateFilter.ALL -> "全部"
            quickFilter == QuickDateFilter.CUSTOM && customDate != null -> {
                val dateStr = java.text.SimpleDateFormat("MM月dd日", Locale.getDefault())
                    .format(Date(customDate))
                if (timeSlot == TimeSlotFilter.ALL) dateStr else "$dateStr ${timeSlot.displayName}"
            }
            timeSlot == TimeSlotFilter.ALL -> quickFilter.displayName
            else -> "${quickFilter.displayName} ${timeSlot.displayName}"
        }
    }
    
    fun hasFilter(): Boolean {
        return quickFilter != QuickDateFilter.ALL || timeSlot != TimeSlotFilter.ALL
    }
}

/**
 * 录音列表ViewModel
 */
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    application: Application,
    private val recordingRepository: RecordingRepository,
    private val debugLogger: com.voicelife.assistant.utils.DebugLogger
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecordingsUiState())
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    private val audioPlayer = AudioPlayer(application)
    
    // Whisper 引擎用于手动转录
    private val whisperEngine = com.voicelife.assistant.transcription.WhisperEngine(application)
    private var isWhisperInitialized = false

    init {
        loadRecordings()
    }
    
    /**
     * 设置快捷日期筛选
     */
    fun setQuickDateFilter(filter: QuickDateFilter) {
        val newCriteria = if (filter == QuickDateFilter.ALL) {
            FilterCriteria()  // 重置所有筛选
        } else {
            _uiState.value.filterCriteria.copy(
                quickFilter = filter,
                customDate = if (filter != QuickDateFilter.CUSTOM) null else _uiState.value.filterCriteria.customDate
            )
        }
        _uiState.value = _uiState.value.copy(filterCriteria = newCriteria)
        applyFilter()
    }
    
    /**
     * 设置自定义日期
     */
    fun setCustomDate(dateMillis: Long) {
        // 将时间戳转换为当天0点
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newCriteria = _uiState.value.filterCriteria.copy(
            quickFilter = QuickDateFilter.CUSTOM,
            customDate = calendar.timeInMillis
        )
        _uiState.value = _uiState.value.copy(filterCriteria = newCriteria)
        applyFilter()
    }
    
    /**
     * 设置时段筛选
     */
    fun setTimeSlotFilter(timeSlot: TimeSlotFilter) {
        val newCriteria = _uiState.value.filterCriteria.copy(timeSlot = timeSlot)
        _uiState.value = _uiState.value.copy(filterCriteria = newCriteria)
        applyFilter()
    }
    
    /**
     * 清除所有筛选
     */
    fun clearFilter() {
        _uiState.value = _uiState.value.copy(filterCriteria = FilterCriteria())
        applyFilter()
    }
    
    /**
     * 应用筛选
     */
    private fun applyFilter() {
        val allRecordings = _uiState.value.recordings
        val criteria = _uiState.value.filterCriteria
        
        val filtered = if (!criteria.hasFilter()) {
            allRecordings
        } else {
            val (dateStart, dateEnd) = getDateRange(criteria)
            allRecordings.filter { recording ->
                val inDateRange = recording.createdAt in dateStart..dateEnd
                val inTimeSlot = isInTimeSlot(recording.createdAt, criteria.timeSlot)
                inDateRange && inTimeSlot
            }
        }
        
        _uiState.value = _uiState.value.copy(filteredRecordings = filtered)
    }
    
    /**
     * 获取日期范围
     */
    private fun getDateRange(criteria: FilterCriteria): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // 今天开始时间
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        return when (criteria.quickFilter) {
            QuickDateFilter.ALL -> 0L to now
            QuickDateFilter.TODAY -> todayStart to now
            QuickDateFilter.YESTERDAY -> {
                val yesterdayStart = todayStart - 24 * 60 * 60 * 1000
                yesterdayStart to todayStart
            }
            QuickDateFilter.LAST_7_DAYS -> {
                val weekAgo = todayStart - 7 * 24 * 60 * 60 * 1000
                weekAgo to now
            }
            QuickDateFilter.LAST_30_DAYS -> {
                val monthAgo = todayStart - 30L * 24 * 60 * 60 * 1000
                monthAgo to now
            }
            QuickDateFilter.CUSTOM -> {
                val customStart = criteria.customDate ?: todayStart
                val customEnd = customStart + 24 * 60 * 60 * 1000
                customStart to customEnd
            }
        }
    }
    
    /**
     * 检查时间是否在指定时段内
     */
    private fun isInTimeSlot(timestamp: Long, timeSlot: TimeSlotFilter): Boolean {
        if (timeSlot == TimeSlotFilter.ALL) return true
        
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return hour >= timeSlot.startHour && hour < timeSlot.endHour
    }
    
    /**
     * 手动触发转录
     */
    fun transcribeRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                debugLogger.i("RecordingsViewModel", "🎯 手动触发转录: ${java.io.File(recording.filePath).name}")
                
                // 更新状态为转录中
                _uiState.value = _uiState.value.copy(
                    transcribingRecordingId = recording.id
                )
                
                // 初始化 Whisper（如果还没初始化）
                if (!isWhisperInitialized) {
                    debugLogger.i("RecordingsViewModel", "🔧 初始化 Whisper 引擎...")
                    val modelPath = whisperEngine.getModelPath()
                    if (!whisperEngine.isModelAvailable(modelPath)) {
                        debugLogger.w("RecordingsViewModel", "📦 复制模型文件...")
                        whisperEngine.copyModelFromAssets()
                    }
                    val initialized = whisperEngine.initialize(modelPath)
                    if (initialized) {
                        isWhisperInitialized = true
                        debugLogger.i("RecordingsViewModel", "✅ Whisper 引擎初始化成功")
                    } else {
                        debugLogger.e("RecordingsViewModel", "❌ Whisper 引擎初始化失败")
                        _uiState.value = _uiState.value.copy(
                            transcribingRecordingId = null,
                            error = "Whisper 引擎初始化失败"
                        )
                        return@launch
                    }
                }
                
                // 执行转录
                debugLogger.i("RecordingsViewModel", "🔄 开始转录...")
                val text = whisperEngine.transcribe(recording.filePath)
                
                if (text != null && text.isNotEmpty()) {
                    // 保存转录结果
                    recordingRepository.updateTranscription(recording.id, text)
                    debugLogger.i("RecordingsViewModel", "✅ 转录完成: ${text.take(50)}...")
                    
                    // 刷新列表
                    loadRecordings()
                } else {
                    debugLogger.w("RecordingsViewModel", "⚠️ 转录返回空结果")
                    _uiState.value = _uiState.value.copy(error = "转录失败：无结果")
                }
                
            } catch (e: Exception) {
                debugLogger.e("RecordingsViewModel", "❌ 转录失败: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "转录失败: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(transcribingRecordingId = null)
            }
        }
    }

    /**
     * 加载录音列表
     */
    fun loadRecordings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val recordings = recordingRepository.getAllRecordings()
                    .sortedByDescending { it.createdAt }  // 按时间倒序排列
                debugLogger.d("RecordingsViewModel", "加载了 ${recordings.size} 条录音")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recordings = recordings
                )
                
                // 应用当前筛选
                applyFilter()
            } catch (e: Exception) {
                debugLogger.e("RecordingsViewModel", "加载录音失败: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * 播放录音
     */
    fun playRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                if (_uiState.value.playingRecordingId == recording.id) {
                    // 如果正在播放这个录音，则暂停
                    audioPlayer.pause()
                    _uiState.value = _uiState.value.copy(
                        playingRecordingId = null,
                        isPlaying = false
                    )
                } else {
                    // 播放新录音
                    audioPlayer.play(recording.filePath) { isPlaying ->
                        _uiState.value = _uiState.value.copy(
                            isPlaying = isPlaying,
                            playingRecordingId = if (isPlaying) recording.id else null
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        playingRecordingId = recording.id,
                        isPlaying = true
                    )
                }
            } catch (e: Exception) {
                debugLogger.e("RecordingsViewModel", "播放失败: ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 停止播放
     */
    fun stopPlaying() {
        audioPlayer.stop()
        _uiState.value = _uiState.value.copy(
            playingRecordingId = null,
            isPlaying = false
        )
    }

    /**
     * 删除录音
     */
    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                val fileName = java.io.File(recording.filePath).name
                recordingRepository.deleteRecording(recording.id)
                debugLogger.i("RecordingsViewModel", "已删除录音: $fileName")
                loadRecordings()
            } catch (e: Exception) {
                debugLogger.e("RecordingsViewModel", "删除失败: ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

/**
 * 录音列表UI状态
 */
data class RecordingsUiState(
    val isLoading: Boolean = false,
    val recordings: List<Recording> = emptyList(),
    val filteredRecordings: List<Recording> = emptyList(),
    val filterCriteria: FilterCriteria = FilterCriteria(),
    val playingRecordingId: Long? = null,
    val isPlaying: Boolean = false,
    val transcribingRecordingId: Long? = null,
    val error: String? = null
)
