# 24小时智能语音助手 - 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 构建一个24小时运行的Android语音助手应用，使用VAD检测人声、自动录音并通过本地Whisper转换为文字。

**架构：** 采用四层模块化架构（VAD检测、录音管理、转换调度、数据管理），由前台服务协调运行。使用Silero VAD进行人声检测，Whisper Base模型本地转换，智能调度策略确保电池安全。

**技术栈：** Android SDK, Kotlin, Room Database, Coroutines, ONNX Runtime (Silero VAD), Whisper.cpp (Base模型), Hilt (依赖注入)

---

## Phase 1: 项目基础设置

### Task 1.1: 创建Android项目结构

**Files:**
- Create: `app/build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`

**Step 1: 创建Gradle构建文件**

创建 `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VoiceLifeAssistant"
include(":app")
```

创建根目录 `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

**Step 2: 创建app模块构建文件**

创建 `app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.voicelife.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.voicelife.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ONNX Runtime (for Silero VAD)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

    // Gson for JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

**Step 3: 创建AndroidManifest.xml**

创建 `app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 权限声明 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".VoiceAssistantApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.VoiceLifeAssistant"
        tools:targetApi="31">

        <activity
            android:name=".ui.main.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.VoiceLifeAssistant">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.VoiceMonitorService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="microphone" />
    </application>
</manifest>
```

**Step 4: 创建Application类**

创建 `app/src/main/java/com/voicelife/assistant/VoiceAssistantApp.kt`:
```kotlin
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
```

**Step 5: 创建基础包结构**

创建以下目录结构：
```
app/src/main/java/com/voicelife/assistant/
├── VoiceAssistantApp.kt
├── data/
│   ├── database/
│   ├── model/
│   └── repository/
├── di/
├── recorder/
├── service/
├── storage/
├── sync/
├── transcription/
├── ui/
│   ├── main/
│   ├── settings/
│   └── history/
├── utils/
└── vad/
```

**Step 6: Commit**

```bash
cd /c/Users/yangyayuan/Desktop/yu/.worktrees/voice-assistant-impl
git add .
git commit -m "feat: 初始化Android项目结构

- 配置Gradle构建系统
- 添加必要依赖（Compose, Room, Hilt, ONNX Runtime）
- 创建AndroidManifest with权限声明
- 设置基础包结构"
```

---

### Task 1.2: 创建数据库模型和DAO

**Files:**
- Create: `app/src/main/java/com/voicelife/assistant/data/model/Recording.kt`
- Create: `app/src/main/java/com/voicelife/assistant/data/model/Transcription.kt`
- Create: `app/src/main/java/com/voicelife/assistant/data/database/RecordingDao.kt`
- Create: `app/src/main/java/com/voicelife/assistant/data/database/TranscriptionDao.kt`
- Create: `app/src/main/java/com/voicelife/assistant/data/database/AppDatabase.kt`

**Step 1: 创建Recording数据模型**

创建 `app/src/main/java/com/voicelife/assistant/data/model/Recording.kt`:
```kotlin
package com.voicelife.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val duration: Int,  // 秒
    val fileSize: Long,  // 字节
    val createdAt: Long,  // Unix timestamp
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val transcribedAt: Long? = null,
    val deleteAt: Long  // 7天后删除
)

enum class TranscriptionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

**Step 2: 创建Transcription数据模型**

创建 `app/src/main/java/com/voicelife/assistant/data/model/Transcription.kt`:
```kotlin
package com.voicelife.assistant.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        ForeignKey(
            entity = Recording::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordingId")]
)
data class Transcription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordingId: Long,
    val text: String,
    val language: String?,
    val segments: String,  // JSON格式
    val createdAt: Long
)
```

**Step 3: 创建RecordingDao**

创建 `app/src/main/java/com/voicelife/assistant/data/database/RecordingDao.kt`:
```kotlin
package com.voicelife.assistant.data.database

import androidx.room.*
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): Recording?

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE transcriptionStatus = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: TranscriptionStatus): List<Recording>

    @Query("SELECT * FROM recordings WHERE deleteAt < :timestamp")
    suspend fun getExpiredRecordings(timestamp: Long): List<Recording>

    @Query("DELETE FROM recordings WHERE deleteAt < :timestamp")
    suspend fun deleteExpired(timestamp: Long): Int
}
```

**Step 4: 创建TranscriptionDao**

创建 `app/src/main/java/com/voicelife/assistant/data/database/TranscriptionDao.kt`:
```kotlin
package com.voicelife.assistant.data.database

import androidx.room.*
import com.voicelife.assistant.data.model.Transcription
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Insert
    suspend fun insert(transcription: Transcription): Long

    @Query("SELECT * FROM transcriptions WHERE recordingId = :recordingId")
    suspend fun getByRecordingId(recordingId: Long): Transcription?

    @Query("SELECT * FROM transcriptions WHERE text LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    fun searchByKeyword(keyword: String): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<Transcription>
}
```

**Step 5: 创建AppDatabase**

创建 `app/src/main/java/com/voicelife/assistant/data/database/AppDatabase.kt`:
```kotlin
package com.voicelife.assistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicelife.assistant.data.model.Recording
import com.voicelife.assistant.data.model.Transcription

@Database(
    entities = [Recording::class, Transcription::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun transcriptionDao(): TranscriptionDao
}
```

**Step 6: 创建数据库依赖注入模块**

创建 `app/src/main/java/com/voicelife/assistant/di/DatabaseModule.kt`:
```kotlin
package com.voicelife.assistant.di

import android.content.Context
import androidx.room.Room
import com.voicelife.assistant.data.database.AppDatabase
import com.voicelife.assistant.data.database.RecordingDao
import com.voicelife.assistant.data.database.TranscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_assistant_db"
        ).build()
    }

    @Provides
    fun provideRecordingDao(database: AppDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    fun provideTranscriptionDao(database: AppDatabase): TranscriptionDao {
        return database.transcriptionDao()
    }
}
```

**Step 7: Commit**

```bash
git add app/src/main/java/com/voicelife/assistant/data/
git add app/src/main/java/com/voicelife/assistant/di/DatabaseModule.kt
git commit -m "feat: 创建数据库模型和DAO

- 定义Recording和Transcription实体
- 创建DAO接口with Flow支持
- 配置Room数据库
- 添加Hilt依赖注入模块"
```

---

## Phase 2: VAD检测模块

### Task 2.1: 集成Silero VAD模型

**Files:**
- Create: `app/src/main/assets/silero_vad.onnx` (需要下载)
- Create: `app/src/main/java/com/voicelife/assistant/vad/VadDetector.kt`
- Create: `app/src/main/java/com/voicelife/assistant/vad/SileroVadEngine.kt`

**Step 1: 下载Silero VAD模型**

模型文件需要从以下地址下载：
https://github.com/snakers4/silero-vad/blob/master/files/silero_vad.onnx

将文件放置到 `app/src/main/assets/silero_vad.onnx`

**Step 2: 创建VAD回调接口**

创建 `app/src/main/java/com/voicelife/assistant/vad/VadCallback.kt`:
```kotlin
package com.voicelife.assistant.vad

interface VadCallback {
    fun onVoiceStart()
    fun onVoiceEnd()
    fun onError(error: Exception)
}
```

**Step 3: 创建Silero VAD引擎**

创建 `app/src/main/java/com/voicelife/assistant/vad/SileroVadEngine.kt`:
```kotlin
package com.voicelife.assistant.vad

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer

class SileroVadEngine(private val context: Context) {
    private var ortSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null

    private val sampleRate = 16000
    private val frameSize = 512  // 样本数

    // 模型状态
    private var h: FloatBuffer? = null
    private var c: FloatBuffer? = null
    private var sr: LongBuffer? = null

    companion object {
        private const val TAG = "SileroVadEngine"
        private const val MODEL_FILENAME = "silero_vad.onnx"
    }

    fun init() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()

            // 从assets加载模型
            val modelBytes = context.assets.open(MODEL_FILENAME).use { it.readBytes() }

            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setIntraOpNumThreads(1)

            ortSession = ortEnvironment!!.createSession(modelBytes, sessionOptions)

            // 初始化状态
            initializeState()

            Log.d(TAG, "Silero VAD initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Silero VAD", e)
            throw e
        }
    }

    private fun initializeState() {
        h = FloatBuffer.allocate(2 * 64)
        c = FloatBuffer.allocate(2 * 64)
        sr = LongBuffer.allocate(1).apply { put(sampleRate.toLong()) }
    }

    fun process(audioFrame: FloatArray): Float {
        try {
            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                FloatBuffer.wrap(audioFrame),
                longArrayOf(1, audioFrame.size.toLong())
            )

            val inputs = mapOf(
                "input" to inputTensor,
                "h" to OnnxTensor.createTensor(ortEnvironment, h, longArrayOf(2, 1, 64)),
                "c" to OnnxTensor.createTensor(ortEnvironment, c, longArrayOf(2, 1, 64)),
                "sr" to OnnxTensor.createTensor(ortEnvironment, sr, longArrayOf(1))
            )

            val output = ortSession!!.run(inputs)

            // 获取输出概率
            val probability = (output[0].value as Array<FloatArray>)[0][0]

            // 更新状态
            h = (output[1].value as FloatBuffer)
            c = (output[2].value as FloatBuffer)

            // 清理
            output.close()
            inputTensor.close()

            return probability
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio frame", e)
            return 0f
        }
    }

    fun reset() {
        initializeState()
    }

    fun release() {
        ortSession?.close()
        ortEnvironment?.close()
        ortSession = null
        ortEnvironment = null
    }
}
```

**Step 4: 创建VAD检测器**

创建 `app/src/main/java/com/voicelife/assistant/vad/VadDetector.kt`:
```kotlin
package com.voicelife.assistant.vad

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.abs

class VadDetector(
    private val context: Context,
    private val callback: VadCallback
) {
    private var audioRecord: AudioRecord? = null
    private var vadEngine: SileroVadEngine? = null

    private var isRunning = false
    private var detectionJob: Job? = null

    private val sampleRate = 16000
    private val frameSize = 512
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // VAD参数
    private val voiceThreshold = 0.5f
    private val minVoiceFrames = 3  // 连续3帧才触发
    private val minSilenceFrames = 30  // 约2秒静音才结束

    private var consecutiveVoiceFrames = 0
    private var consecutiveSilenceFrames = 0
    private var isVoiceActive = false

    companion object {
        private const val TAG = "VadDetector"
    }

    fun init() {
        try {
            vadEngine = SileroVadEngine(context)
            vadEngine?.init()

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            Log.d(TAG, "VAD Detector initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VAD Detector", e)
            callback.onError(e)
        }
    }

    fun start() {
        if (isRunning) return

        isRunning = true
        audioRecord?.startRecording()

        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            detectVoiceActivity()
        }

        Log.d(TAG, "VAD detection started")
    }

    private suspend fun detectVoiceActivity() {
        val audioBuffer = ShortArray(frameSize)
        val floatBuffer = FloatArray(frameSize)

        while (isRunning && audioRecord != null) {
            try {
                val readSize = audioRecord!!.read(audioBuffer, 0, frameSize)
                if (readSize <= 0) continue

                // 转换为float [-1, 1]
                for (i in audioBuffer.indices) {
                    floatBuffer[i] = audioBuffer[i] / 32768.0f
                }

                // VAD处理
                val probability = vadEngine?.process(floatBuffer) ?: 0f

                // 状态机处理
                processVadResult(probability)

            } catch (e: Exception) {
                Log.e(TAG, "Error in voice detection", e)
                if (isRunning) {
                    callback.onError(e)
                }
            }
        }
    }

    private fun processVadResult(probability: Float) {
        if (probability > voiceThreshold) {
            // 检测到人声
            consecutiveVoiceFrames++
            consecutiveSilenceFrames = 0

            if (!isVoiceActive && consecutiveVoiceFrames >= minVoiceFrames) {
                // 触发人声开始
                isVoiceActive = true
                callback.onVoiceStart()
                Log.d(TAG, "Voice activity started")
            }
        } else {
            // 静音
            consecutiveSilenceFrames++
            consecutiveVoiceFrames = 0

            if (isVoiceActive && consecutiveSilenceFrames >= minSilenceFrames) {
                // 触发人声结束
                isVoiceActive = false
                callback.onVoiceEnd()
                Log.d(TAG, "Voice activity ended")
            }
        }
    }

    fun stop() {
        isRunning = false
        detectionJob?.cancel()
        audioRecord?.stop()
        consecutiveVoiceFrames = 0
        consecutiveSilenceFrames = 0
        isVoiceActive = false
        vadEngine?.reset()
        Log.d(TAG, "VAD detection stopped")
    }

    fun release() {
        stop()
        audioRecord?.release()
        vadEngine?.release()
        audioRecord = null
        vadEngine = null
        Log.d(TAG, "VAD Detector released")
    }
}
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/voicelife/assistant/vad/
git commit -m "feat: 集成Silero VAD检测模块

- 封装ONNX Runtime for Silero VAD
- 实现实时人声检测状态机
- 添加防抖动逻辑（连续3帧触发）
- 支持回调接口for录音管理"
```

---

## Phase 3: 录音管理模块

### Task 3.1: 实现音频录制器

**Files:**
- Create: `app/src/main/java/com/voicelife/assistant/recorder/AudioRecorder.kt`
- Create: `app/src/main/java/com/voicelife/assistant/recorder/RecordingSession.kt`
- Create: `app/src/main/java/com/voicelife/assistant/recorder/WavFileWriter.kt`

**Step 1: 创建WAV文件写入器**

创建 `app/src/main/java/com/voicelife/assistant/recorder/WavFileWriter.kt`:
```kotlin
package com.voicelife.assistant.recorder

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileWriter(private val outputFile: File) {
    private val sampleRate = 16000
    private val channels = 1
    private val bitsPerSample = 16

    private var fileOutputStream: FileOutputStream? = null
    private var dataSize = 0L

    fun start() {
        fileOutputStream = FileOutputStream(outputFile)
        // 先写入占位的WAV头（稍后更新）
        writeWavHeader(0)
    }

    fun write(audioData: ShortArray) {
        val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (sample in audioData) {
            byteBuffer.putShort(sample)
        }

        fileOutputStream?.write(byteBuffer.array())
        dataSize += audioData.size * 2L
    }

    fun stop() {
        fileOutputStream?.close()

        // 更新WAV头with正确的文件大小
        updateWavHeader()
    }

    private fun writeWavHeader(dataLength: Long) {
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        header.put("RIFF".toByteArray())
        header.putInt((36 + dataLength).toInt())
        header.put("WAVE".toByteArray())

        // fmt chunk
        header.put("fmt ".toByteArray())
        header.putInt(16)  // fmt chunk size
        header.putShort(1)  // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * channels * bitsPerSample / 8)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())

        // data chunk
        header.put("data".toByteArray())
        header.putInt(dataLength.toInt())

        fileOutputStream?.write(header.array())
    }

    private fun updateWavHeader() {
        try {
            RandomAccessFile(outputFile, "rw").use { raf ->
                // 更新文件大小
                raf.seek(4)
                raf.write(intToByteArray((36 + dataSize).toInt()))

                // 更新data chunk大小
                raf.seek(40)
                raf.write(intToByteArray(dataSize.toInt()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
}
```

**Step 2: 创建录音会话**

创建 `app/src/main/java/com/voicelife/assistant/recorder/RecordingSession.kt`:
```kotlin
package com.voicelife.assistant.recorder

import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingSession(
    private val recordingsDir: File,
    private val onRecordingComplete: (File) -> Unit
) {
    private var wavWriter: WavFileWriter? = null
    private var currentFile: File? = null
    private var lastVoiceTime = 0L
    private var silenceCheckJob: Job? = null

    private val preBuffer = RingBuffer(16000 * 2)  // 1秒预缓冲
    private val silenceGapMs = 10000L  // 10秒静音间隔
    private val postBufferMs = 3000L  // 3秒后缓冲

    companion object {
        private const val TAG = "RecordingSession"
    }

    fun onVoiceStart(preBufferData: ShortArray?) {
        if (wavWriter != null) {
            // 已经在录音中，重置静音计时
            lastVoiceTime = System.currentTimeMillis()
            silenceCheckJob?.cancel()
            return
        }

        // 开始新的录音
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        currentFile = File(recordingsDir, "pending/voice_$timestamp.wav")
        currentFile?.parentFile?.mkdirs()

        wavWriter = WavFileWriter(currentFile!!)
        wavWriter?.start()

        // 写入预缓冲数据
        preBufferData?.let {
            wavWriter?.write(it)
        }

        lastVoiceTime = System.currentTimeMillis()
        Log.d(TAG, "Recording started: ${currentFile?.name}")
    }

    fun writeAudioData(audioData: ShortArray) {
        // 更新预缓冲
        preBuffer.write(audioData)

        // 如果正在录音，写入文件
        wavWriter?.write(audioData)
    }

    fun onVoiceEnd() {
        lastVoiceTime = System.currentTimeMillis()

        // 开始检查静音间隔
        silenceCheckJob = CoroutineScope(Dispatchers.IO).launch {
            delay(silenceGapMs)

            // 10秒后仍无人声，停止录音
            stopRecording()
        }
    }

    fun onVoiceContinue() {
        // 重置静音检查
        silenceCheckJob?.cancel()
        lastVoiceTime = System.currentTimeMillis()
    }

    private fun stopRecording() {
        val file = currentFile ?: return

        // 继续录制后缓冲（3秒）
        CoroutineScope(Dispatchers.IO).launch {
            delay(postBufferMs)

            wavWriter?.stop()
            wavWriter = null

            Log.d(TAG, "Recording completed: ${file.name}, size: ${file.length()}")
            onRecordingComplete(file)

            currentFile = null
        }
    }

    fun forceStop() {
        silenceCheckJob?.cancel()
        wavWriter?.stop()
        wavWriter = null
        currentFile = null
    }
}

// 环形缓冲区
class RingBuffer(private val capacity: Int) {
    private val buffer = ShortArray(capacity)
    private var writePos = 0

    fun write(data: ShortArray) {
        for (sample in data) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % capacity
        }
    }

    fun read(): ShortArray {
        val result = ShortArray(capacity)
        var readPos = writePos
        for (i in result.indices) {
            result[i] = buffer[readPos]
            readPos = (readPos + 1) % capacity
        }
        return result
    }
}
```

**Step 3: 创建音频录制器**

创建 `app/src/main/java/com/voicelife/assistant/recorder/AudioRecorder.kt`:
```kotlin
package com.voicelife.assistant.recorder

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.voicelife.assistant.vad.VadCallback
import com.voicelife.assistant.vad.VadDetector
import kotlinx.coroutines.*
import java.io.File

class AudioRecorder(
    private val context: Context,
    private val recordingsDir: File
) {
    private var vadDetector: VadDetector? = null
    private var recordingSession: RecordingSession? = null
    private var isRecording = false

    private val vadCallback = object : VadCallback {
        override fun onVoiceStart() {
            Log.d(TAG, "VAD: Voice detected")
            recordingSession?.onVoiceStart(null)
        }

        override fun onVoiceEnd() {
            Log.d(TAG, "VAD: Voice ended")
            recordingSession?.onVoiceEnd()
        }

        override fun onError(error: Exception) {
            Log.e(TAG, "VAD error", error)
        }
    }

    companion object {
        private const val TAG = "AudioRecorder"
    }

    fun init() {
        recordingsDir.mkdirs()
        File(recordingsDir, "pending").mkdirs()
        File(recordingsDir, "processing").mkdirs()
        File(recordingsDir, "completed").mkdirs()
        File(recordingsDir, "failed").mkdirs()

        vadDetector = VadDetector(context, vadCallback)
        vadDetector?.init()
    }

    fun start(onRecordingComplete: (File) -> Unit) {
        if (isRecording) return

        recordingSession = RecordingSession(recordingsDir, onRecordingComplete)
        vadDetector?.start()
        isRecording = true

        Log.d(TAG, "Audio recorder started")
    }

    fun stop() {
        if (!isRecording) return

        vadDetector?.stop()
        recordingSession?.forceStop()
        isRecording = false

        Log.d(TAG, "Audio recorder stopped")
    }

    fun release() {
        stop()
        vadDetector?.release()
        vadDetector = null
        recordingSession = null
    }
}
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/voicelife/assistant/recorder/
git commit -m "feat: 实现录音管理模块

- 创建WAV文件写入器with正确的文件头
- 实现智能合并策略（10秒静音间隔）
- 支持预缓冲和后缓冲
- 集成VAD检测器触发录音"
```

---

*（由于篇幅限制，完整的实施计划继续包含以下阶段）*

## Phase 4: 转换调度模块
- Task 4.1: 集成Whisper.cpp
- Task 4.2: 实现设备状态监控
- Task 4.3: 实现转换调度器

## Phase 5: 数据管理与存储
- Task 5.1: 实现Repository层
- Task 5.2: 实现存储管理器
- Task 5.3: 实现自动清理策略

## Phase 6: 前台服务
- Task 6.1: 实现VoiceMonitorService
- Task 6.2: 实现通知管理
- Task 6.3: 实现保活策略

## Phase 7: UI界面
- Task 7.1: 主界面（Jetpack Compose）
- Task 7.2: 设置界面
- Task 7.3: 历史记录与搜索

## Phase 8: 集成测试与优化
- Task 8.1: 端到端测试
- Task 8.2: 性能优化
- Task 8.3: 电量优化测试

---

**注意事项：**

1. **Whisper模型集成**：需要使用whisper.cpp的Android JNI封装，或使用已有的Android Whisper库如`whisper-android`
2. **权限处理**：在MainActivity中添加运行时权限请求逻辑
3. **电池优化**：引导用户将应用加入电池优化白名单
4. **测试**：每个Phase完成后进行单元测试和集成测试

**下一步：** 选择执行方式（子代理驱动 or 并行会话）
