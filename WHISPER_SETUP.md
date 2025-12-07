# Whisper 本地语音识别设置指南

## 功能说明

VoiceLife现在支持本地语音识别功能，使用Whisper.cpp将录音文件转换成文字。

## 工作流程

1. **录音** - VAD检测到人声，自动录制音频文件
2. **保存** - 录音保存到`/sdcard/Download/VoiceAssistant/pending/`
3. **转录** - 转录服务自动将录音转换成文字
4. **完成** - 转录文本保存到数据库，状态更新为COMPLETED

## Whisper模型下载

### 推荐模型：ggml-tiny.bin

**tiny模型特点：**
- 文件大小：~75MB
- 速度：最快
- 准确度：中等
- 适合：实时转录、移动设备

### 下载地址

**方法1：从Hugging Face下载**
```bash
# 下载tiny模型
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin

# 或使用curl
curl -L -o ggml-tiny.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin
```

**方法2：从GitHub下载**
```bash
# 克隆whisper.cpp仓库
git clone https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp

# 下载模型
bash ./models/download-ggml-model.sh tiny
```

### 其他可用模型

| 模型 | 大小 | 速度 | 准确度 | 适用场景 |
|------|------|------|--------|----------|
| tiny | 75MB | 最快 | 中等 | 实时转录 |
| base | 142MB | 快 | 良好 | 日常使用 |
| small | 466MB | 中等 | 很好 | 高质量转录 |
| medium | 1.5GB | 慢 | 优秀 | 专业转录 |
| large | 2.9GB | 很慢 | 最佳 | 最高质量 |

## 模型安装

### 方法1：放入assets目录（推荐）

1. 下载`ggml-tiny.bin`模型文件
2. 将文件放入项目的`app/src/main/assets/`目录
3. 重新构建APK
4. 应用首次启动时会自动复制到内部存储

```
app/
  src/
    main/
      assets/
        ggml-tiny.bin  <-- 放这里
```

### 方法2：手动推送到设备

```bash
# 下载模型
wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin

# 推送到设备
adb push ggml-tiny.bin /data/data/com.voicelife.assistant/files/
```

### 方法3：从网络下载（需要实现）

可以在应用中添加下载功能，首次启动时从网络下载模型。

## 使用说明

### 自动转录

1. 启动VoiceLife应用
2. 点击"开始监听"
3. 对着手机说话
4. 录音完成后自动加入转录队列
5. 转录服务在后台处理
6. 查看转录结果

### 查看转录文本

在录音列表中可以看到每条录音的转录文本。

### 转录状态

- **PENDING** - 待转录（黄色）
- **PROCESSING** - 转录中（蓝色）
- **COMPLETED** - 已完成（绿色）
- **FAILED** - 转录失败（红色）

## 性能优化

### 转录速度

- **tiny模型**：约1-2秒/分钟音频
- **base模型**：约2-4秒/分钟音频
- **small模型**：约5-10秒/分钟音频

### 内存占用

- **tiny模型**：~200MB RAM
- **base模型**：~300MB RAM
- **small模型**：~600MB RAM

### 电池消耗

转录过程会消耗CPU资源，建议：
- 使用tiny或base模型
- 在充电时进行批量转录
- 设置转录队列处理间隔

## 配置选项

### 修改模型

在`WhisperEngine.kt`中修改：

```kotlin
companion object {
    private const val MODEL_NAME = "ggml-tiny.bin"  // 改成其他模型
}
```

### 修改处理间隔

在`TranscriptionService.kt`中修改：

```kotlin
companion object {
    private const val PROCESS_INTERVAL = 5000L  // 5秒，可以调整
}
```

## 故障排除

### 问题1：模型文件未找到

**错误**：`Model file not found`

**解决**：
1. 检查模型文件是否在assets目录
2. 或手动推送到设备
3. 检查文件权限

### 问题2：转录失败

**错误**：`Transcription failed`

**解决**：
1. 检查音频文件是否完整
2. 确认模型文件正确
3. 查看日志获取详细错误

### 问题3：转录速度慢

**解决**：
1. 使用更小的模型（tiny）
2. 减少同时处理的文件数
3. 在性能更好的设备上运行

### 问题4：内存不足

**错误**：`OutOfMemoryError`

**解决**：
1. 使用tiny模型
2. 关闭其他应用
3. 增加处理间隔

## 日志查看

### 应用内日志

在VoiceLife应用底部的实时日志面板查看：
- `✅ Whisper引擎初始化成功`
- `🎯 开始转录: voice_xxx.wav`
- `✅ 转录完成: 转录文本...`

### ADB日志

```bash
adb logcat -s WhisperEngine:* TranscriptionService:*
```

## 高级功能

### 批量转录

```kotlin
// 重试所有失败的转录
transcriptionService.retryFailed()
```

### 自定义转录参数

可以在`WhisperEngine.kt`中添加更多参数：
- 语言设置
- 翻译功能
- 时间戳
- 说话人识别

## 性能测试

### 测试命令

```bash
# 查看转录性能
adb logcat -s TranscriptionService:* | grep "转录完成"

# 查看内存占用
adb shell dumpsys meminfo com.voicelife.assistant

# 查看CPU占用
adb shell top | grep voicelife
```

## 未来改进

- [ ] 支持多语言识别
- [ ] 添加翻译功能
- [ ] 实时转录（边录边转）
- [ ] 云端转录备选方案
- [ ] 转录质量评分
- [ ] 自动标点符号
- [ ] 说话人分离

## 参考资料

- [Whisper.cpp GitHub](https://github.com/ggerganov/whisper.cpp)
- [Whisper模型下载](https://huggingface.co/ggerganov/whisper.cpp)
- [Whisper论文](https://arxiv.org/abs/2212.04356)

## 总结

Whisper本地语音识别功能已经集成到VoiceLife中，可以自动将录音转换成文字。只需下载模型文件并放入正确位置，即可开始使用。推荐使用tiny模型以获得最佳性能和速度。
