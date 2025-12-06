# 如何获取APK

## 方法1：从GitHub Actions下载（推荐）

1. 访问：https://github.com/naixidananhai/voicenote/actions

2. 点击最新的成功构建（绿色✓）

3. 滚动到页面底部 "Artifacts" 部分

4. 下载 `app-debug.apk`

5. 传输到手机并安装

## 方法2：本地构建（需要完整SDK）

你的SDK不完整，需要在Android Studio中下载：

1. 打开Android Studio
2. Tools → SDK Manager
3. SDK Platforms标签页：
   - 勾选 "Android 14.0 (API 34)"
   - 点击Apply下载
4. SDK Tools标签页：
   - 勾选 "Android SDK Build-Tools 34"
   - 点击Apply下载
5. 下载完成后运行：
   ```cmd
   gradlew assembleDebug
   ```

## 当前问题

你的SDK目录：
- ✅ Build Tools 36.1.0 已安装
- ❌ Android Platform 34 缺失
- ❌ Android Platform 36 未完全下载

## 推荐流程

1. **立即测试**：从GitHub Actions下载APK
2. **查看日志**：安装后测试，复制日志给我
3. **分析问题**：我根据日志修复VAD问题
4. **后续开发**：如需本地开发，再完善SDK

## GitHub Actions构建状态

最新提交已触发自动构建，预计5-10分钟完成。

访问链接查看：https://github.com/naixidananhai/voicenote/actions
