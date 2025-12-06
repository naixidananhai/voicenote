# Silero VAD 模型说明

## 模型信息

**模型名称**: Silero VAD (Voice Activity Detection)
**文件名**: `silero_vad.onnx`
**大小**: 约 1.8 MB
**格式**: ONNX
**版本**: v4.0

## 下载模型

由于模型文件较大,未包含在仓库中。请从以下地址下载:

### 方式 1: 直接下载 (推荐)
```
https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

### 方式 2: 使用wget/curl
```bash
cd app/src/main/assets/
wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

或者:
```bash
cd app/src/main/assets/
curl -L -o silero_vad.onnx https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

## 模型规格

### 输入
- **input**: `[1, N]` - 音频样本, N通常为512
  - 数据类型: float32
  - 值域: [-1.0, 1.0]
  - 采样率: 16kHz

- **h**: `[2, 1, 64]` - LSTM隐藏状态
  - 数据类型: float32

- **c**: `[2, 1, 64]` - LSTM细胞状态
  - 数据类型: float32

- **sr**: `[1]` - 采样率
  - 数据类型: int64
  - 值: 16000

### 输出
- **output**: `[1, 1]` - 人声概率
  - 数据类型: float32
  - 值域: [0.0, 1.0]
  - 阈值建议: 0.5

- **hn**: `[2, 1, 64]` - 更新后的隐藏状态
- **cn**: `[2, 1, 64]` - 更新后的细胞状态

## 性能指标

- **推理延迟**: < 1ms (CPU)
- **CPU占用**: 2-5% (单核)
- **内存占用**: ~10MB
- **准确率**: > 95% (在干净音频上)

## 许可证

Silero VAD 使用 MIT 许可证。

更多信息请访问: https://github.com/snakers4/silero-vad

## 验证模型

下载完成后,验证文件大小:
```bash
ls -lh silero_vad.onnx
# 应该显示约 1.8M
```

验证MD5 (可选):
```bash
md5sum silero_vad.onnx
# 根据版本不同,MD5值可能不同
```

## 集成说明

模型已在 `SileroVadEngine.kt` 中集成。确保模型文件放置在:
```
app/src/main/assets/silero_vad.onnx
```

应用会在运行时从assets目录加载模型。
