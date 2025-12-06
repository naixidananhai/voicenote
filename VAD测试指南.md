# VAD 功能测试指南

## 问题描述

之前测试时发现 VAD 概率一直显示 0.000，即使在说话时也不变化。

## 新增诊断日志

我已经添加了详细的诊断日志来帮助定位问题：

### 1. 音频数据统计（每100帧）
- **原始音频范围**: Short 类型的最大值、最小值、平均值
- **Float 音频范围**: 转换后的 Float 数据范围
- **目的**: 确认麦克风是否正常采集到音频数据

### 2. VAD 引擎诊断（每100帧）
- **音频统计**: Max, Min, Avg, RMS
- **目的**: 确认送入 VAD 的数据是否正常

### 3. VAD 推理结果（每50帧）
- **VAD 概率**: 模型输出的人声概率
- **帧数计数**: 已处理的总帧数
- **目的**: 确认 VAD 模型是否正常工作

## 测试步骤

### 1. 安装最新 APK

从以下位置获取：
- **本地构建**: `app\build\outputs\apk\debug\app-debug.apk`
- **GitHub Actions**: https://github.com/naixidananhai/voicenote/actions

### 2. 授予权限

打开应用后，授予以下权限：
- ✅ 录音权限
- ✅ 通知权限
- ✅ 后台运行权限

### 3. 启动服务

点击"启动服务"按钮，观察日志输出。

### 4. 测试录音

**静音测试**（30秒）：
- 保持安静，不要说话
- 观察日志中的音频数据统计
- **预期**: 音频数据应该接近 0，VAD 概率应该很低（< 0.1）

**说话测试**（30秒）：
- 对着手机说话（正常音量）
- 观察日志中的音频数据和 VAD 概率变化
- **预期**: 
  - 音频数据应该有明显波动
  - VAD 概率应该上升（> 0.5）
  - 应该看到"🎤 检测到人声，开始录音"

### 5. 复制日志

点击"📋 复制"按钮，将所有日志复制出来。

## 关键日志标识

查找以下关键日志：

### ✅ 正常初始化
```
[INFO ] [AudioRecorder] ✅ VAD检测器初始化成功
[INFO ] [AudioRecorder] ✅ 录制器初始化成功 (16kHz, MONO)
[INFO ] [SileroVadEngine] ✅ Silero VAD initialized successfully
```

### 📊 音频数据统计
```
[DEBUG] [AudioRecorder] 原始音频 - Short范围: [-1234, 5678], 平均: 123.45
[DEBUG] [AudioRecorder] Float音频 - 范围: [-0.037, 0.173]
[DEBUG] [SileroVadEngine] 音频统计 - Max: 0.173, Min: -0.037, Avg: 0.004, RMS: 0.023
```

### 🎯 VAD 推理结果
```
[DEBUG] [SileroVadEngine] VAD推理成功 - 概率: 0.856, 帧数: 150
[DEBUG] [AudioRecorder] VAD概率: 0.856
```

### 🎤 人声检测
```
[INFO ] [AudioRecorder] 🎤 检测到人声，开始录音
[INFO ] [RecordingSession] 📝 开始新录音: recording_20251206_160530.wav
```

## 可能的问题和解决方案

### 问题 1: 音频数据全是 0
**症状**: 
```
原始音频 - Short范围: [0, 0], 平均: 0.0
```

**原因**: 麦克风权限未授予或麦克风硬件问题

**解决**: 
1. 检查应用权限设置
2. 重启应用
3. 尝试其他录音应用测试麦克风

### 问题 2: VAD 模型未加载
**症状**:
```
[ERROR] [SileroVadEngine] ❌ Failed to initialize Silero VAD
```

**原因**: ONNX 模型文件缺失或损坏

**解决**:
1. 检查 `app/src/main/assets/silero_vad.onnx` 是否存在
2. 重新构建 APK

### 问题 3: VAD 概率始终为 0
**症状**:
```
[DEBUG] [AudioRecorder] VAD概率: 0.000
```

**可能原因**:
1. 音频数据格式不正确
2. ONNX Runtime 推理失败
3. 模型输入参数错误

**诊断**:
- 查看是否有 "VAD推理成功" 日志
- 检查音频统计数据是否正常
- 查看是否有异常堆栈信息

## 预期的正常日志示例

```
12:00:00.123 [INFO ] [MainViewModel] 应用启动
12:00:00.456 [INFO ] [MainViewModel] 权限检查: 已授予
12:00:01.789 [INFO ] [AudioRecorder] ✅ VAD检测器初始化成功
12:00:01.890 [INFO ] [AudioRecorder] ✅ 录制器初始化成功 (16kHz, MONO)
12:00:02.000 [INFO ] [AudioRecorder] VAD检测已启动，监听中...
12:00:03.123 [DEBUG] [AudioRecorder] 已处理 100 帧音频
12:00:03.124 [DEBUG] [AudioRecorder] 原始音频 - Short范围: [-234, 456], 平均: 12.3
12:00:03.125 [DEBUG] [AudioRecorder] Float音频 - 范围: [-0.007, 0.014]
12:00:03.456 [DEBUG] [AudioRecorder] VAD概率: 0.023
12:00:05.678 [DEBUG] [SileroVadEngine] 音频统计 - Max: 0.234, Min: -0.189, Avg: 0.012, RMS: 0.045
12:00:05.789 [DEBUG] [SileroVadEngine] VAD推理成功 - 概率: 0.856, 帧数: 150
12:00:05.890 [DEBUG] [AudioRecorder] VAD概率: 0.856
12:00:06.000 [INFO ] [AudioRecorder] 🎤 检测到人声，开始录音
12:00:06.123 [INFO ] [RecordingSession] 📝 开始新录音: recording_20251206_120006.wav
```

## 下一步

1. 安装最新 APK
2. 按照测试步骤操作
3. 复制完整日志
4. 分析日志找出问题根源
5. 根据诊断结果调整代码

## 联系方式

如果遇到问题，请提供：
1. 完整的日志输出（点击"📋 复制"）
2. 测试时的操作步骤
3. 手机型号和 Android 版本
