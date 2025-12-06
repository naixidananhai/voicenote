@echo off
REM Android SDK 命令行安装脚本（使用国内镜像）
setlocal enabledelayedexpansion

echo ========================================
echo Android SDK 自动安装工具
echo 使用腾讯云镜像加速下载
echo ========================================
echo.

REM 设置SDK路径
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set SDKMANAGER=%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat

echo SDK路径: %ANDROID_SDK_ROOT%
echo.

REM 检查sdkmanager是否存在
if not exist "%SDKMANAGER%" (
    echo [警告] sdkmanager不存在，尝试查找其他位置...
    
    REM 尝试其他可能的路径
    if exist "%ANDROID_SDK_ROOT%\tools\bin\sdkmanager.bat" (
        set SDKMANAGER=%ANDROID_SDK_ROOT%\tools\bin\sdkmanager.bat
        echo 找到: %SDKMANAGER%
    ) else (
        echo [错误] 未找到sdkmanager工具
        echo.
        echo 请先安装Android Studio或下载command-line tools:
        echo https://developer.android.com/studio#command-tools
        echo.
        echo 或使用国内镜像:
        echo https://mirrors.cloud.tencent.com/AndroidSDK/
        pause
        exit /b 1
    )
)

echo 使用sdkmanager: %SDKMANAGER%
echo.

REM 设置代理为空（避免使用系统代理）
set HTTP_PROXY=
set HTTPS_PROXY=
set http_proxy=
set https_proxy=

echo ========================================
echo 开始安装必需的SDK组件...
echo ========================================
echo.

REM 接受许可证
echo y | "%SDKMANAGER%" --licenses

echo.
echo [1/6] 安装 Platform Tools...
call "%SDKMANAGER%" "platform-tools"

echo.
echo [2/6] 安装 Build Tools 34.0.0...
call "%SDKMANAGER%" "build-tools;34.0.0"

echo.
echo [3/6] 安装 Android 14 (API 34) Platform...
call "%SDKMANAGER%" "platforms;android-34"

echo.
echo [4/6] 安装 Android 13 (API 33) Platform...
call "%SDKMANAGER%" "platforms;android-33"

echo.
echo [5/6] 安装 CMake...
call "%SDKMANAGER%" "cmake;3.22.1"

echo.
echo [6/6] 安装 NDK...
call "%SDKMANAGER%" "ndk;25.1.8937393"

echo.
echo ========================================
echo 安装完成！
echo ========================================
echo.
echo 已安装的组件:
"%SDKMANAGER%" --list_installed
echo.
pause
