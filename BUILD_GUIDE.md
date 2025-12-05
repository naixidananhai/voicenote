# Android APK 编译打包指南

**不需要Android Studio！** 本文档提供两种编译方式。

---

## 🚀 方式1: GitHub Actions 自动编译 (推荐)

**优点**:
- ✅ 无需配置本地环境
- ✅ 云端自动编译
- ✅ 支持多分支并行编译
- ✅ 自动保存APK文件

### 使用步骤：

#### 1. 推送代码到GitHub

```bash
cd .worktrees/voice-assistant-impl

# 确保模型文件在.gitignore中（已配置）
git add -A
git commit -m "chore: 准备编译"
git push origin feature/voice-assistant-impl
```

#### 2. 触发自动编译

**方式A: 推送触发（自动）**
- 推送代码后自动开始编译
- 约5-10分钟完成

**方式B: 手动触发**
1. 访问GitHub仓库页面
2. 点击 `Actions` 标签
3. 选择 `Android CI - Build APK` 工作流
4. 点击 `Run workflow` 按钮
5. 选择分支后点击 `Run workflow`

#### 3. 下载编译好的APK

1. 进入 `Actions` 页面
2. 找到最新的成功构建（绿色✓）
3. 点击进入详情页
4. 在 `Artifacts` 部分下载：
   - **app-debug.apk** - Debug版本（推荐测试用）
   - **app-release-unsigned.apk** - Release版本（未签名）

#### 4. 安装APK

```bash
# 传输到手机后直接安装
# 或通过adb安装
adb install app-debug.apk
```

---

## 💻 方式2: 本地命令行编译

### 前提条件

#### 必需软件：

1. **JDK 17**
   - 下载: https://adoptium.net/
   - 配置环境变量 `JAVA_HOME`

2. **Android SDK** (命令行工具)
   - 下载: https://developer.android.com/studio#command-line-tools-only
   - 或使用sdkmanager安装

3. **模型文件** (必需)
   - `app/src/main/assets/silero_vad.onnx` (217KB)

#### 验证环境：

```bash
# 检查Java版本
java -version
# 应该显示: openjdk version "17.x.x"

# 检查JAVA_HOME
echo $JAVA_HOME  # Linux/Mac
echo %JAVA_HOME%  # Windows
```

---

### 编译步骤

#### 1. 进入项目目录

```bash
cd .worktrees/voice-assistant-impl
```

#### 2. 配置Android SDK路径

**选项A: 使用环境变量**
```bash
# Linux/Mac
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Windows
set ANDROID_HOME=C:\path\to\android-sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools
```

**选项B: 创建local.properties**
```bash
# 在项目根目录创建local.properties文件
echo "sdk.dir=/path/to/android-sdk" > local.properties
```

#### 3. 授予Gradle执行权限 (Linux/Mac)

```bash
chmod +x gradlew
```

#### 4. 编译APK

**编译Debug版本** (推荐，用于测试)
```bash
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**编译Release版本** (生产环境)
```bash
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

#### 5. 查看编译结果

编译成功后，APK文件位置：

```
Debug版本:
app/build/outputs/apk/debug/app-debug.apk

Release版本:
app/build/outputs/apk/release/app-release-unsigned.apk
```

#### 6. 安装到手机

**方式A: 通过adb安装**
```bash
# 连接手机并启用USB调试
adb devices

# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 或强制覆盖安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**方式B: 传输到手机安装**
```bash
# 将APK复制到手机
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/

# 在手机文件管理器中找到APK并安装
```

---

## 🔧 常见问题

### Q1: gradlew命令找不到

**A**:
```bash
# 确保在项目根目录
cd .worktrees/voice-assistant-impl

# 检查文件是否存在
ls gradlew  # Linux/Mac
dir gradlew.bat  # Windows
```

### Q2: 提示SDK未找到

**A**: 需要配置Android SDK路径，参考上面"配置Android SDK路径"部分。

或者使用GitHub Actions自动编译（推荐）。

### Q3: 编译失败 - "JAVA_HOME not set"

**A**:
```bash
# 设置JAVA_HOME环境变量
export JAVA_HOME=/path/to/jdk-17  # Linux/Mac
set JAVA_HOME=C:\path\to\jdk-17   # Windows

# 验证
echo $JAVA_HOME  # Linux/Mac
echo %JAVA_HOME%  # Windows
```

### Q4: 编译时间太长

**A**:
- 首次编译会下载依赖，需要10-20分钟
- 后续编译会使用缓存，约1-3分钟
- 使用 `--offline` 可跳过依赖检查（仅缓存后可用）

### Q5: GitHub Actions编译失败

**A**:
1. 检查分支名是否正确
2. 查看Actions日志定位错误
3. 确保代码已正确推送
4. 检查是否有.gitignore排除了必要文件

### Q6: Release版本需要签名怎么办？

**A**: Release APK需要签名才能安装：

**方法1: 使用Debug版本**（推荐测试）
```bash
# Debug版本已自动签名
./gradlew assembleDebug
```

**方法2: 生成签名文件**
```bash
# 创建keystore
keytool -genkey -v -keystore my-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias my-key-alias

# 配置build.gradle.kts使用签名
# （需要修改配置文件）
```

---

## 📦 编译输出说明

### Debug APK
- **文件**: `app-debug.apk`
- **用途**: 开发测试
- **签名**: 自动使用debug签名
- **可直接安装**: ✅

### Release APK (unsigned)
- **文件**: `app-release-unsigned.apk`
- **用途**: 生产发布
- **签名**: 未签名
- **需要签名后才能安装**: ⚠️

---

## 🎯 推荐方案

### 对于测试和个人使用：
✅ **使用GitHub Actions编译Debug版本**
- 无需配置环境
- 自动编译
- 下载即用

### 对于正式发布：
📱 **本地编译Release版本并签名**
- 完整控制编译过程
- 正式签名
- 可上架应用商店

---

## 📝 快速命令参考

```bash
# GitHub Actions编译（推荐）
git push origin feature/voice-assistant-impl
# 然后在GitHub页面下载APK

# 本地快速编译
cd .worktrees/voice-assistant-impl
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# 清理编译缓存
./gradlew clean

# 查看所有可用任务
./gradlew tasks

# 检查依赖
./gradlew dependencies

# 查看项目属性
./gradlew properties
```

---

## 🔍 验证APK

编译完成后验证APK：

```bash
# 查看APK信息
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# 查看文件大小
ls -lh app/build/outputs/apk/debug/app-debug.apk

# 验证签名（仅Release）
jarsigner -verify -verbose -certs app-release.apk
```

---

## ✨ 小贴士

1. **首选GitHub Actions**: 最简单，无需配置环境
2. **模型文件**: 确保silero_vad.onnx已放置在assets目录
3. **网络问题**: 首次编译需要下载依赖，确保网络通畅
4. **使用Debug版**: 测试阶段推荐使用Debug版本
5. **adb工具**: 可以从Android SDK或platform-tools单独下载

---

**推荐流程**: 推送代码 → GitHub自动编译 → 下载APK → 安装测试 🚀
