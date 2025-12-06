@echo off
REM Android Studio 国内镜像启动脚本
REM 使用腾讯云镜像加速SDK下载

echo ========================================
echo Android Studio 国内镜像启动器
echo ========================================
echo.

REM 设置镜像环境变量
set ANDROID_SDK_MIRROR=mirrors.cloud.tencent.com
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk

echo 配置信息:
echo - SDK路径: %ANDROID_SDK_ROOT%
echo - 镜像地址: %ANDROID_SDK_MIRROR%
echo.

REM 查找Android Studio安装路径
set AS_PATH=
if exist "C:\Program Files\Android\Android Studio\bin\studio64.exe" (
    set AS_PATH=C:\Program Files\Android\Android Studio\bin\studio64.exe
)
if exist "%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe" (
    set AS_PATH=%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe
)
if exist "C:\Program Files (x86)\Android\Android Studio\bin\studio64.exe" (
    set AS_PATH=C:\Program Files (x86)\Android\Android Studio\bin\studio64.exe
)

if "%AS_PATH%"=="" (
    echo [错误] 未找到Android Studio安装路径
    echo 请手动指定路径或在Android Studio中配置镜像
    pause
    exit /b 1
)

echo - Android Studio: %AS_PATH%
echo.
echo 正在启动Android Studio...
echo.

start "" "%AS_PATH%"

echo Android Studio已启动
echo 在SDK Manager中下载时会使用国内镜像
pause
