# 模型下载指南

本项目需要两个AI模型才能完整运行：

## 📦 必需模型

### 1. Silero VAD 模型 (必需 - Phase 2已完成)

**用途**: 实时人声活动检测 (Voice Activity Detection)

**文件信息**:
- 文件名: `silero_vad.onnx`
- 大小: 约 1.8 MB (217 KB)
- 格式: ONNX
- 版本: v4.0

**存放位置**:
```
.worktrees/voice-assistant-impl/app/src/main/assets/silero_vad.onnx
```

**下载方式**:

#### 方式 1: 直接下载 (推荐)
访问以下地址直接下载：
```
https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

#### 方式 2: 使用命令行
```bash
cd .worktrees/voice-assistant-impl/app/src/main/assets/
curl -L -O https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

或使用wget:
```bash
cd .worktrees/voice-assistant-impl/app/src/main/assets/
wget https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

#### 方式 3: 使用国内镜像 (如果GitHub访问慢)
```bash
# 使用ghproxy镜像
curl -L -o silero_vad.onnx https://ghproxy.com/https://github.com/snakers4/silero-vad/raw/master/files/silero_vad.onnx
```

**验证下载**:
```bash
# 检查文件大小（应该是 217088 字节）
ls -lh app/src/main/assets/silero_vad.onnx

# 验证文件类型
file app/src/main/assets/silero_vad.onnx
# 应该显示: data
```

---

## 📦 可选模型

### 2. Whisper Base 模型 (可选 - Phase 4转换功能)

**用途**: 语音转文字 (Speech to Text)

**模型信息**:
- 模型名称: Whisper Base
- 大小: 约 140 MB
- 格式: GGML (whisper.cpp格式)
- 版本: Base (多种量化版本可选)

**存放位置** (待Phase 4实现时确定):
```
.worktrees/voice-assistant-impl/app/src/main/assets/ggml-base.bin
```

**下载方式**:

#### 方式 1: 从whisper.cpp官方下载
```bash
# Base模型 (140MB)
curl -L -O https://huggingface.co/ggerganov/whisper.ggml/resolve/main/ggml-base.bin

# 或者使用量化版本（更小但精度略降）
# Base.q5 (57MB) - 推荐平衡版本
curl -L -O https://huggingface.co/ggerganov/whisper.ggml/resolve/main/ggml-base.q5_0.bin
```

#### 方式 2: 使用Hugging Face镜像
```bash
# 使用国内镜像
curl -L -O https://hf-mirror.com/ggerganov/whisper.ggml/resolve/main/ggml-base.bin
```

#### Whisper模型版本对比

| 模型 | 大小 | 速度 | 精度 | 推荐场景 |
|------|------|------|------|----------|
| tiny | 75 MB | 非常快 | 低 | 测试/演示 |
| base | 140 MB | 快 | 中 | **推荐** 日常使用 |
| small | 460 MB | 中 | 高 | 高精度需求 |
| medium | 1.5 GB | 慢 | 很高 | 专业场景 |
| large | 2.9 GB | 很慢 | 最高 | 不推荐移动端 |

**量化版本说明**:
- `.q5_0` - 5-bit量化，大小减少约60%，精度损失很小
- `.q8_0` - 8-bit量化，大小减少约30%，精度损失极小
- 无后缀 - FP16原始精度

---

## 🚀 快速开始

### 当前可运行的功能 (只需Silero VAD)

下载Silero VAD模型后，应用已可运行以下功能：
- ✅ 24小时VAD人声检测
- ✅ 自动录音和智能合并
- ✅ WAV文件生成
- ✅ 数据库管理
- ✅ 存储空间管理
- ✅ 前台服务运行

### Phase 4 完整功能 (需要Whisper模型)

下载Whisper模型后，可实现：
- ⏳ 语音转文字
- ⏳ 智能调度转换
- ⏳ 文本搜索和查询
- ⏳ AI分析准备

---

## 📋 下载检查清单

- [ ] 下载Silero VAD模型 (217KB)
- [ ] 将模型放到 `app/src/main/assets/silero_vad.onnx`
- [ ] 验证文件大小和完整性
- [ ] (可选) 下载Whisper Base模型 (140MB)
- [ ] (可选) 将Whisper模型放到指定位置

---

## ❓ 常见问题

### Q: GitHub下载速度很慢怎么办？
**A**: 使用国内镜像：
- ghproxy: `https://ghproxy.com/` + 原始链接
- Hugging Face镜像: `https://hf-mirror.com/`

### Q: 如何验证模型下载正确？
**A**:
```bash
# Silero VAD应该是217088字节
ls -l app/src/main/assets/silero_vad.onnx

# Whisper Base应该约140MB
ls -lh app/src/main/assets/ggml-base.bin
```

### Q: 可以使用其他Whisper模型吗？
**A**: 可以！tiny/small/medium都支持，但需要在代码中修改模型路径和配置。Base模型是性能和精度的最佳平衡。

### Q: 模型会上传到Git仓库吗？
**A**: 不会，模型文件已在`.gitignore`中排除。

---

## 📚 参考资源

- **Silero VAD**: https://github.com/snakers4/silero-vad
- **Whisper**: https://github.com/openai/whisper
- **Whisper.cpp**: https://github.com/ggerganov/whisper.cpp
- **Whisper模型下载**: https://huggingface.co/ggerganov/whisper.ggml

---

## 🔧 技术细节

### Silero VAD规格
- 输入: 16kHz, 512样本/帧, float32 [-1, 1]
- 输出: 人声概率 [0.0, 1.0]
- 延迟: < 100ms
- CPU占用: 2-5%

### Whisper Base规格
- 输入: 16kHz, WAV格式
- 输出: 文本 + 时间戳
- 处理速度: 约1x实时 (1分钟音频需1-2分钟)
- 支持语言: 99种（包括中文）
- 精度: WER ~10-15% (中文)

---

**下载完成后即可编译运行应用！** 🎉
