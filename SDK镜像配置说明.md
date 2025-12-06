# Android SDK 国内镜像配置指南

## 问题
Android Studio的SDK Manager从 `dl.google.com` 下载，国内访问困难。

## 解决方案

### 方案1：修改hosts文件（最简单）

1. 以管理员身份打开记事本
2. 打开文件：`C:\Windows\System32\drivers\etc\hosts`
3. 添加以下内容：

```
# Google Hosts
203.208.40.33 dl.google.com
203.208.40.33 dl-ssl.google.com
```

4. 保存文件
5. 重启Android Studio

### 方案2：使用代理工具

如果你有代理工具（如Clash、V2Ray等）：

1. 打开Android Studio
2. File → Settings → Appearance & Behavior → System Settings → HTTP Proxy
3. 选择 "Manual proxy configuration"
4. 填入你的代理地址和端口
5. 点击 "Check connection" 测试
6. 点击 OK保存

### 方案3：使用Android Studio内置镜像配置

1. 打开Android Studio
2. File → Settings → Appearance & Behavior → System Settings → Android SDK
3. 点击 "SDK Update Sites" 标签页
4. 添加以下镜像站点：

**腾讯云镜像：**
- Name: Tencent Mirror
- URL: https://mirrors.cloud.tencent.com/AndroidSDK/repository/repository2-1.xml

**阿里云镜像：**
- Name: Aliyun Mirror  
- URL: https://maven.aliyun.com/repository/google

5. 取消勾选官方源
6. 点击 OK保存

### 方案4：手动下载SDK组件（最可靠）

从国内镜像站手动下载所需组件：

**腾讯云镜像站：**
https://mirrors.cloud.tencent.com/AndroidSDK/

**需要的组件：**

1. **Command Line Tools**
   - 文件：commandlinetools-win-*_latest.zip
   - 解压到：`%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\`

2. **Platform Tools**
   - 文件：platform-tools_r*-windows.zip
   - 解压到：`%LOCALAPPDATA%\Android\Sdk\platform-tools\`

3. **Build Tools 34.0.0**
   - 文件：build-tools_r34-windows.zip
   - 解压到：`%LOCALAPPDATA%\Android\Sdk\build-tools\34.0.0\`

4. **Android 14 Platform (API 34)**
   - 文件：platform-34_r*.zip
   - 解压到：`%LOCALAPPDATA%\Android\Sdk\platforms\android-34\`

### 方案5：使用GitHub Actions构建（推荐）

不需要本地SDK，直接使用GitHub Actions：

1. 访问：https://github.com/naixidananhai/voicenote/actions
2. 查看最新的构建任务
3. 下载构建好的 `app-debug.apk`
4. 安装到手机测试

## 当前项目状态

✅ 代码已推送到GitHub
✅ GitHub Actions会自动构建APK
✅ 不需要本地SDK也能获得APK

## 推荐流程

1. 先从GitHub Actions下载APK测试功能
2. 如果需要本地开发，再配置SDK镜像
3. 使用方案1（修改hosts）最简单有效

## 验证SDK是否安装成功

在项目目录运行：
```cmd
gradlew assembleDebug
```

如果构建成功，说明SDK配置正确。
