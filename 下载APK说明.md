# 下载 APK 说明

## 本地构建的 APK

APK 已经成功构建！

**文件位置**: `app\build\outputs\apk\debug\app-debug.apk`
**文件大小**: 74.8 MB
**生成时间**: 2025/12/6 15:50

### 安装方法

1. 将 APK 文件传输到你的 Android 手机
2. 在手机上打开文件管理器，找到 APK 文件
3. 点击安装（可能需要允许"未知来源"安装）

## GitHub Actions 自动构建

每次推送代码到 GitHub，Actions 会自动构建 APK。

### 下载步骤

1. 访问: https://github.com/naixidananhai/voicenote/actions
2. 点击最新的 "Android CI" workflow
3. 等待构建完成（绿色✓）
4. 在页面底部的 "Artifacts" 区域下载 `app-debug`

## 构建配置说明

### JDK 版本要求

- 本地构建需要 JDK 17（已配置自动下载）
- GitHub Actions 使用 JDK 17

### 构建命令

```bash
# 本地构建
.\gradlew.bat assembleDebug

# 清理后构建
.\gradlew.bat clean assembleDebug
```

### SDK 要求

- Android SDK Platform 34
- Build Tools 36.1.0
- 最低支持 Android 8.0 (API 26)
- 目标版本 Android 14 (API 34)

## 功能特性

- ✅ 实时语音活动检测 (VAD)
- ✅ 自动录音保存
- ✅ 实时调试日志显示
- ✅ 录音统计和存储管理
- ✅ 前台服务持续监听

## 下一步

安装 APK 后，测试录音功能并查看日志输出，特别关注 VAD 概率是否正常工作。
