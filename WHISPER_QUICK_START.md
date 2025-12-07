# Whisper 集成快速开始

## 🚀 5分钟快速集成

### 第一步：下载模型文件

**选择一个模型下载：**

#### 方法1：从 Hugging Face 下载（推荐）
```powershell
# 在项目根目录执行
cd app\src\main\assets

# 下载 base 模型（142MB）
curl -L -o ggml-base.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin

# 或下载 tiny 模型（75MB，更快）
# curl -L -o ggml-tiny.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

#### 方法2：从桌面的 whisper.cpp 项目下载
```powershell
# 使用 whisper.cpp 的下载脚本
cd C:\Users\yangyayuan\Desktop\whisper.cpp
bash models/download-ggml-model.sh base

# 复制到项目
copy models\ggml-base.bin C:\Users\yangyayuan\Desktop\yu\app\src\main\assets\
```

#### 方法3：手动下载
1. 访问：https://huggingface.co/ggerganov/whisper.cpp/tree/main
2. 下载 `ggml-base.bin`
3. 放入 `app/src/main/assets/` 目录

### 第二步：确认文件结构

确保以下文件存在：
```
app/src/main/assets/
└── ggml-base.bin  (约 142MB)

whisper-lib/
├── build.gradle.kts
└── src/main/
    ├── java/com/whispercpp/whisper/
    │   ├── LibWhisper.kt
    │   └── WhisperCpuConfig.kt
    └── jni/whisper/
        ├── CMakeLists.txt
        ├── jni.c
        └── whisper-cpp/
            ├── src/
            ├── ggml/
            └── include/
```

### 第三步：构建项目

```powershell
# 清理项目
.\gradlew clean

# 构建 whisper-lib（首次需要 10-20 分钟）
.\gradlew :whisper-lib:build

# 构建 APK
.\gradlew :app:assembleDebug
```

**注意：** 首次编译 whisper.cpp 会很慢，这是正常的！

### 第四步：安装测试

```powershell
# 安装到手机
adb install app\build\outputs\apk\debug\app-debug.apk

# 查看日志
adb logcat -s WhisperEngine:* TranscriptionService:* LibWhisper:*
```

## 🎯 验证集成

### 1. 启动应用
- 应用启动时会自动复制模型文件
- 查看日志：`✅ Model copied successfully: 142MB`

### 2. 开始录音
- 点击"开始监听"
- 说话触发录音
- 录音完成后自动转录

### 3. 查看结果
- 在录音列表中查看转录文本
- 或查看日志：`✅ Transcription completed`

## ⚠️ 常见问题

### Q1: 编译失败 - NDK not found
**A:** 安装 NDK
```
Android Studio → Tools → SDK Manager → SDK Tools
勾选 "NDK (Side by side)" → Apply
```

### Q2: 编译失败 - CMake not found
**A:** 安装 CMake
```
Android Studio → Tools → SDK Manager → SDK Tools
勾选 "CMake" → Apply
```

### Q3: 编译太慢
**A:** 这是正常的！首次编译 whisper.cpp 需要：
- Debug 模式：10-15 分钟
- Release 模式：15-20 分钟
- 后续编译会快很多（1-3 分钟）

### Q4: 内存不足
**A:** 增加 Gradle 内存
```properties
# 在 gradle.properties 中添加
org.gradle.jvmargs=-Xmx4096m
```

### Q5: 模型文件太大
**A:** 使用更小的模型
```kotlin
// 在 WhisperEngine.kt 中修改
private const val MODEL_NAME = "ggml-tiny.bin"  // 75MB
```

## 🔧 高级配置

### 修改语言设置
编辑 `whisper-lib/src/main/jni/whisper/jni.c`：
```c
// 第 150 行左右
params.language = "zh";  // 中文
// params.language = "en";  // 英文
```

### 修改线程数
编辑 `whisper-lib/src/main/java/com/whispercpp/whisper/LibWhisper.kt`：
```kotlin
// 第 20 行左右
val numThreads = 4  // 手动设置线程数
// val numThreads = WhisperCpuConfig.preferredThreadCount  // 自动检测
```

### 启用时间戳
编辑 `whisper-lib/src/main/jni/whisper/jni.c`：
```c
params.print_timestamps = true;  // 启用时间戳
```

## 📊 性能对比

| 模型 | 大小 | 速度 | 准确度 | 推荐场景 |
|------|------|------|--------|----------|
| tiny | 75MB | 最快 | 中等 | 实时转录、低端设备 |
| base | 142MB | 快 | 良好 | **日常使用（推荐）** |
| small | 466MB | 中等 | 很好 | 高质量转录 |
| medium | 1.5GB | 慢 | 优秀 | 专业转录 |

## 🎉 成功标志

如果看到以下日志，说明集成成功：

```
WhisperEngine: ✅ Model copied successfully: 142MB
WhisperEngine: Loading Whisper model from: /data/user/0/.../ggml-base.bin
LibWhisper: Loading libwhisper.so
WhisperEngine: ✅ Whisper engine initialized successfully
TranscriptionService: ✅ Whisper引擎初始化成功
TranscriptionService: 🎯 开始转录: voice_xxx.wav
WhisperEngine: ✅ Transcription completed in 2345ms
TranscriptionService: ✅ 转录完成: 你好，这是一个测试
```

## 📞 需要帮助？

如果遇到问题：

1. **查看完整日志**
   ```bash
   adb logcat > logcat.txt
   ```

2. **检查文件**
   ```bash
   # 检查模型文件
   adb shell ls -lh /data/data/com.voicelife.assistant/files/
   
   # 检查 .so 文件
   adb shell ls -lh /data/app/com.voicelife.assistant-*/lib/arm64/
   ```

3. **清理重试**
   ```bash
   .\gradlew clean
   .\gradlew :whisper-lib:build --stacktrace
   ```

---

**祝你集成顺利！** 🚀
