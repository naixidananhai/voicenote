# 24小时智能语音助手 - 设计文档

**项目名称：** VoiceLifeAssistant（暂定）
**版本：** v1.0
**日期：** 2025-12-04
**平台：** Android

## 一、项目概述

### 1.1 项目目标

开发一款24小时运行的智能录音应用，通过VAD技术检测人声并自动录音，使用本地Whisper模型转换为文字，为AI分析提供丰富的生活上下文，从而在工作、生活、家庭、爱情、健康、职业规划、技能学习、人际关系等方面提供个性化建议。

### 1.2 核心特性

- ✅ 24小时后台VAD检测，低功耗运行
- ✅ 智能录音合并，捕获完整对话段落
- ✅ 本地Whisper转换，保护隐私
- ✅ 智能调度策略，安全优先（避免充电时过热）
- ✅ 灵活数据管理，支持本地/云端配置
- ✅ 音频自动清理，文字长期保存

### 1.3 使用场景

全天候环境监听，记录所有包含人声的对话片段，为LLM提供丰富的个人生活上下文，实现真正个性化的AI助手建议。

---

## 二、系统架构

### 2.1 整体架构

采用**四层模块化架构**，由一个前台服务（ForegroundService）协调运行：

```
┌─────────────────────────────────────┐
│      VoiceMonitorService            │
│         (前台服务)                   │
└─────────────────────────────────────┘
            │
            ├─► VAD检测层（实时检测）
            │   └─ Silero VAD
            │
            ├─► 录音管理层（音频采集）
            │   └─ AudioRecord + 智能合并
            │
            ├─► 转换调度层（文字转换）
            │   └─ Whisper Base + 智能调度
            │
            └─► 数据管理层（存储管理）
                └─ SQLite + 文件管理
```

### 2.2 模块职责

| 模块 | 职责 | 关键技术 |
|------|------|----------|
| VAD检测层 | 实时监听麦克风，检测人声活动 | Silero VAD, ONNX Runtime |
| 录音管理层 | 采集音频，智能合并对话段 | AudioRecord, WAV编码 |
| 转换调度层 | 根据设备状态调度Whisper转换 | Whisper.cpp, 状态监控 |
| 数据管理层 | 存储音频/文字，自动清理 | SQLite, 文件管理 |

---

## 三、核心模块详细设计

### 3.1 VAD检测模块

#### 技术选型：Silero VAD

- **模型大小：** ~1MB
- **支持语言：** 包括中文
- **检测延迟：** <100ms
- **CPU占用：** 持续运行约2-5%

#### 实现细节

```kotlin
class VadDetector {
    // 初始化
    fun init() {
        // 加载Silero VAD ONNX模型
        // 使用ONNX Runtime Mobile
    }

    // 音频输入
    fun processAudioFrame(audioData: ShortArray) {
        // 输入：16kHz, 512样本/帧
        // 检测逻辑：
        // - 概率阈值：>0.5视为人声
        // - 防抖动：连续3帧才触发开始
        // - 静音检测：连续30帧（~2秒）才触发停止
    }

    // 回调接口
    interface VadCallback {
        fun onVoiceStart()
        fun onVoiceEnd()
    }
}
```

#### 电量优化

- 使用低功耗音频采集模式（VOICE_RECOGNITION）
- 模型推理使用NNAPI硬件加速（如果设备支持）
- 静音时降低采样检测频率（从16kHz降到8kHz）

---

### 3.2 录音管理模块

#### 录音配置

```
音频格式：
├─ 采样率：16kHz（Whisper要求）
├─ 声道：单声道（MONO）
├─ 编码：16bit PCM
├─ 缓冲区：4096字节/帧
└─ 输出格式：WAV（带44字节头）
```

#### 智能合并策略

```kotlin
class RecordingSession {
    // 预缓冲：环形缓冲区保存最近1秒音频
    private val preBuffer = RingBuffer(16000 * 2) // 1秒

    // 触发录音：VAD检测到人声
    fun onVoiceStart() {
        // 1. 将预缓冲内容写入文件
        // 2. 开始持续录音
    }

    // 静音处理
    fun onVoiceEnd() {
        // 静音<10秒：继续录制（可能是短暂停顿）
        // 静音≥10秒：停止录音
        // 后缓冲：停止前继续录3秒
    }

    // 文件命名：voice_yyyyMMdd_HHmmss.wav
}
```

#### 存储结构

```
/data/data/[app]/files/recordings/
├─ pending/      # 待转换的音频
├─ processing/   # 转换中的音频
├─ completed/    # 已转换（保留7天）
└─ failed/       # 转换失败（手动处理）
```

#### 异常处理

- **麦克风被占用：** 显示通知，等待释放
- **存储空间不足（<500MB）：** 暂停录音，清理旧文件
- **录音权限丢失：** 发送通知提醒用户

---

### 3.3 转换调度模块

#### 设备状态监控

```kotlin
class DeviceStateMonitor {
    // 监控项
    data class DeviceState(
        val batteryLevel: Int,        // 电量百分比
        val isCharging: Boolean,       // 是否充电
        val isScreenOn: Boolean,       // 屏幕状态
        val cpuTemperature: Float      // CPU温度
    )

    // 每30秒更新一次状态
    fun observeState(): Flow<DeviceState>
}
```

#### 调度策略引擎

```
策略判断流程：

1. 检查充电状态
   └─ 如果充电中 → 拒绝处理（安全优先，避免过热）

2. 检查CPU温度
   └─ 如果>45°C → 延迟处理（防止过热）

3. 检查电量和屏幕
   ├─ 电量>50% AND 息屏 → 立即处理模式
   │  ├─ 从pending队列取任务
   │  ├─ 单线程顺序处理
   │  └─ 每处理一个检查一次状态
   │
   └─ 其他情况 → 延迟到夜间（2-5点）
      └─ 使用AlarmManager设置唤醒
```

#### Whisper转换引擎

```kotlin
class WhisperEngine {
    // 基于whisper.cpp

    // 模型：Base模型（~74MB）
    // 量化：INT8量化减少内存占用
    // 线程：单线程处理（避免过载）

    // 输入：16kHz WAV文件
    fun transcribe(audioFile: File): TranscriptionResult

    // 输出：JSON格式
    data class TranscriptionResult(
        val text: String,              // 转写文本
        val segments: List<Segment>,   // 时间戳分段
        val language: String           // 检测到的语言
    )

    // 处理时长：约1分钟音频需1-2分钟处理（接近实时）
    // 性能优化：使用NNAPI/GPU加速（如果设备支持）
}
```

#### 任务队列管理

```kotlin
class TranscriptionQueue {
    // 基于SQLite
    // 字段：id, audio_path, status, created_at, priority
    // 状态：pending → processing → completed/failed
    // 优先级：最旧的优先处理（FIFO）
    // 失败重试：最多3次，间隔1小时
}
```

---

### 3.4 数据管理模块

#### 数据库设计（SQLite）

```sql
-- 录音记录表
CREATE TABLE recordings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path TEXT NOT NULL,              -- 音频文件路径
    duration INTEGER,                      -- 时长（秒）
    file_size INTEGER,                     -- 文件大小（字节）
    created_at INTEGER NOT NULL,           -- 录音时间戳
    transcription_status TEXT DEFAULT 'pending',  -- pending/processing/completed/failed
    transcribed_at INTEGER,                -- 转写完成时间
    delete_at INTEGER                      -- 计划删除时间（7天后）
);

-- 转写文本表
CREATE TABLE transcriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    recording_id INTEGER NOT NULL,         -- 关联录音ID
    text TEXT NOT NULL,                    -- 转写文本
    language TEXT,                         -- 检测到的语言
    segments TEXT,                         -- JSON格式的时间戳分段
    created_at INTEGER NOT NULL,
    FOREIGN KEY (recording_id) REFERENCES recordings(id)
);

-- 应用配置表
CREATE TABLE app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

#### 存储管理策略

```kotlin
class StorageManager {
    // 空间监控
    fun monitorStorage() {
        // 每小时检查可用空间
        // 警戒线：500MB
        // 触发清理：<500MB时清理最旧文件
    }

    // 自动清理
    fun scheduledCleanup() {
        // 每天凌晨3点执行
        // 删除7天前的音频文件
        // 保留所有转写文本（占用小）
        // 清理失败的临时文件
    }

    // 导出功能
    fun exportTranscriptions(
        startDate: Long,
        endDate: Long,
        format: ExportFormat  // JSON/TXT/CSV
    ): File
}
```

#### 云端同步（可选配置）

```kotlin
class SyncManager {
    // 同步策略
    data class SyncConfig(
        val wifiOnly: Boolean = true,
        val minBatteryLevel: Int = 30,
        val syncContent: SyncContent  // TEXT_ONLY, TEXT_AND_AUDIO, LOCAL_ONLY
    )

    // 同步内容（用户可配置）
    enum class SyncContent {
        TEXT_ONLY,           // 仅文本（推荐，数据量小）
        TEXT_AND_AUDIO,      // 文本+音频（完整备份）
        LOCAL_ONLY           // 完全本地（不同步）
    }

    // 接口设计
    // - RESTful API或自定义服务器
    // - 支持增量同步
    // - 端到端加密（可选）
}
```

---

## 四、前台服务与系统集成

### 4.1 前台服务设计

```kotlin
class VoiceMonitorService : Service() {

    override fun onCreate() {
        // 初始化所有模块
        vadDetector.init()
        audioRecorder.init()
        transcriptionScheduler.init()
        storageManager.init()

        // 注册系统广播监听
        registerBatteryReceiver()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建前台通知
        startForeground(NOTIFICATION_ID, createNotification())

        // 启动VAD检测线程
        vadDetector.start()

        // 启动状态监控
        deviceStateMonitor.start()

        return START_STICKY  // 确保服务被杀后重启
    }

    override fun onDestroy() {
        vadDetector.stop()
        audioRecorder.stop()
        // 保存状态并释放资源
    }
}
```

### 4.2 通知设计

```kotlin
class NotificationHelper {

    // 常驻通知（可最小化）
    fun createStatusNotification(state: ServiceState): Notification {
        // 标题："语音助手正在监听"
        // 内容：动态显示状态
        //   - 待机："等待人声..."
        //   - 录音："正在录音 [时长]"
        //   - 转换："处理中 (队列: 3)"
        //   - 电量提示："电量低，已暂停处理"

        // 优先级：LOW（不打扰用户）

        // 操作按钮：
        //   - 暂停/恢复监听
        //   - 停止服务
    }

    // 提醒通知
    fun showWarningNotification(type: WarningType) {
        // 存储空间不足
        // 权限丢失
        // 转换失败
    }
}
```

### 4.3 权限管理

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**运行时权限请求流程：**
1. 首次启动引导用户授权
2. 录音权限：运行时请求
3. 电池优化：引导用户加入白名单
4. 通知权限：Android 13+请求

### 4.4 保活策略

```
多重保活机制：
├─ 前台服务（主要，最稳定）
├─ START_STICKY重启
├─ JobScheduler定期检查
├─ 监听系统广播（开机、解锁）
└─ WakeLock部分唤醒（仅VAD检测时）
```

---

## 五、用户界面设计

### 5.1 主界面（MainActivity）

```
界面布局：

┌──────────────────────────────────┐
│  顶部状态卡片                      │
│  ├─ 服务状态：运行中/已停止         │
│  ├─ 今日录音：X段，共X分钟          │
│  ├─ 待处理：X个文件                │
│  └─ 存储占用：XXX MB              │
├──────────────────────────────────┤
│  控制区                           │
│  ├─ [开始/停止监听] (大按钮)       │
│  ├─ [暂停监听] (临时静音)          │
│  └─ [立即处理队列] (手动触发)      │
├──────────────────────────────────┤
│  历史记录列表                      │
│  ├─ 按日期分组                    │
│  ├─ 时间 | 时长 | 文本预览        │
│  ├─ 点击查看详情                  │
│  ├─ 长按：删除/重转/分享           │
│  └─ 🔍 搜索框（关键词搜索）        │
├──────────────────────────────────┤
│  [首页] [统计] [设置]              │
└──────────────────────────────────┘
```

### 5.2 设置界面（SettingsActivity）

```kotlin
// 设置选项组织
object Settings {

    // VAD设置
    object Vad {
        var sensitivity: Sensitivity = Sensitivity.MEDIUM  // 低/中/高
        var silenceGap: Int = 10  // 静音间隔：5-30秒
    }

    // 录音设置
    object Recording {
        var sampleRate: Int = 16000  // 8kHz/16kHz
        var retentionDays: Int = 7   // 音频保留：3/7/15/30天
    }

    // 转换设置
    object Transcription {
        var batteryThreshold: Int = 50        // 电量阈值：30%/50%/70%
        var nightProcessingStart: Int = 2     // 夜间开始时间
        var nightProcessingEnd: Int = 5       // 夜间结束时间
        var languagePreference: Language = Language.AUTO  // 自动/中文/英文
    }

    // 数据管理
    object Data {
        var syncMode: SyncMode = SyncMode.LOCAL_ONLY
        fun exportData(format: ExportFormat)
        fun clearCache()
        fun showStorageDetails()
    }

    // 其他
    object Other {
        var autoStart: Boolean = true
        var notificationConfig: NotificationConfig
        fun showPrivacyPolicy()
    }
}
```

---

## 六、错误处理与日志

### 6.1 错误处理策略

```
异常分级处理：

├─ 致命错误（停止服务）
│  ├─ 录音权限永久拒绝
│  ├─ 存储空间完全耗尽
│  └─ Whisper模型文件损坏
│
├─ 严重错误（暂停功能）
│  ├─ 麦克风被占用
│  ├─ 存储空间<100MB
│  └─ 连续转换失败>5次
│
├─ 一般错误（重试）
│  ├─ 单个文件转换失败
│  ├─ 临时网络错误（同步时）
│  └─ 数据库写入失败
│
└─ 警告（记录日志）
   ├─ VAD检测偶发异常
   ├─ CPU温度过高
   └─ 电量低于阈值
```

### 6.2 日志系统

```kotlin
object AppLogger {
    // 日志级别
    enum class Level { DEBUG, INFO, WARN, ERROR }

    // 日志文件
    // 路径：/files/logs/
    // 滚动策略：每天一个文件
    // 保留时长：7天
    // 单文件上限：5MB

    // 关键事件记录
    fun logServiceEvent(event: ServiceEvent)
    fun logRecordingEvent(event: RecordingEvent)
    fun logTranscriptionEvent(event: TranscriptionEvent)
    fun logError(error: Throwable, context: String)
    fun logPerformance(metrics: PerformanceMetrics)

    // 隐私保护：不记录音频内容和转写文本
}
```

### 6.3 性能监控

```kotlin
class PerformanceMonitor {
    data class Metrics(
        val cpuUsage: Float,           // CPU使用率
        val memoryUsage: Long,         // 内存占用（字节）
        val batteryDrain: Float,       // 电池消耗（%/小时）
        val recordingDuration: Long,   // 录音总时长
        val transcriptionSpeed: Float  // 转换速度（实时倍率）
    )

    // 每小时生成性能报告
    fun generateHourlyReport(): Metrics
}
```

---

## 七、技术栈总结

### 7.1 核心依赖

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| VAD引擎 | Silero VAD + ONNX Runtime | 轻量级人声检测 |
| 语音转文字 | Whisper.cpp (Base模型) | 本地离线转换 |
| 数据库 | SQLite + Room | 元数据管理 |
| 音频采集 | AudioRecord API | 原生Android API |
| 后台服务 | ForegroundService | 前台服务保活 |
| 依赖注入 | Hilt | 模块化架构 |
| 异步处理 | Kotlin Coroutines + Flow | 协程和响应式编程 |

### 7.2 项目结构

```
com.voicelife.assistant/
├─ ui/
│  ├─ main/           # 主界面
│  ├─ settings/       # 设置界面
│  └─ history/        # 历史记录
│
├─ service/
│  └─ VoiceMonitorService.kt  # 前台服务
│
├─ vad/
│  ├─ VadDetector.kt
│  └─ SileroVadEngine.kt
│
├─ recorder/
│  ├─ AudioRecorder.kt
│  └─ RecordingSession.kt
│
├─ transcription/
│  ├─ WhisperEngine.kt
│  ├─ TranscriptionScheduler.kt
│  └─ TranscriptionQueue.kt
│
├─ data/
│  ├─ database/       # Room数据库
│  ├─ repository/     # 数据仓库
│  └─ model/          # 数据模型
│
├─ storage/
│  └─ StorageManager.kt
│
├─ sync/
│  └─ SyncManager.kt
│
└─ utils/
   ├─ AppLogger.kt
   ├─ PerformanceMonitor.kt
   └─ NotificationHelper.kt
```

---

## 八、开发路线图

### Phase 1: 基础框架（Week 1-2）
- [ ] 项目初始化，配置依赖
- [ ] 前台服务搭建
- [ ] 权限管理实现
- [ ] 基础UI框架

### Phase 2: 核心功能（Week 3-4）
- [ ] VAD检测模块集成
- [ ] 录音管理模块
- [ ] Whisper引擎集成
- [ ] 数据库设计与实现

### Phase 3: 智能调度（Week 5）
- [ ] 设备状态监控
- [ ] 转换调度策略
- [ ] 存储管理与清理

### Phase 4: 用户界面（Week 6）
- [ ] 主界面完善
- [ ] 设置界面
- [ ] 历史记录与搜索

### Phase 5: 优化与测试（Week 7-8）
- [ ] 性能优化
- [ ] 电量优化
- [ ] 稳定性测试
- [ ] 长时间运行测试

### Phase 6: 云端同步（可选）
- [ ] 同步接口实现
- [ ] 数据加密
- [ ] 增量同步

---

## 九、风险与挑战

### 9.1 技术风险

| 风险项 | 影响 | 应对措施 |
|-------|------|---------|
| Android系统杀后台 | 服务中断 | 多重保活策略 + 用户引导加白名单 |
| Whisper处理速度慢 | 队列积压 | 智能调度 + Base模型优化 |
| 电池消耗大 | 用户体验差 | VAD优化 + 智能处理策略 |
| 存储空间快速占满 | 功能受限 | 自动清理 + 用户提醒 |
| 不同设备兼容性 | 部分设备不可用 | 兼容性测试 + 降级方案 |

### 9.2 隐私风险

- **数据敏感性高：** 记录用户全天对话
- **应对措施：**
  - 本地优先处理
  - 明确隐私协议
  - 用户完全控制数据
  - 支持随时删除所有数据

---

## 十、总结

本设计提供了一个完整的24小时智能语音助手架构方案，核心特点：

✅ **模块化架构** - 各模块职责清晰，易于维护扩展
✅ **安全优先** - 充电时禁止处理，避免过热风险
✅ **智能调度** - 根据设备状态灵活处理任务
✅ **隐私保护** - 本地处理为主，用户完全控制数据
✅ **用户体验** - 前台服务保证稳定性，通知可最小化

该方案平衡了功能需求、性能表现、电量消耗和用户体验，为后续AI分析提供丰富的个人上下文数据基础。
