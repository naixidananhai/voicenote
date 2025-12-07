@echo off
echo ========================================
echo Whisper 集成构建脚本
echo ========================================
echo.

echo [1/4] 检查模型文件...
if exist "app\src\main\assets\ggml-base.bin" (
    echo ✅ 模型文件已就位
) else (
    echo ❌ 模型文件不存在！
    echo 请先复制 ggml-base.bin 到 app\src\main\assets\
    pause
    exit /b 1
)
echo.

echo [2/4] 清理项目...
call gradlew clean
if errorlevel 1 (
    echo ❌ 清理失败
    pause
    exit /b 1
)
echo ✅ 清理完成
echo.

echo [3/4] 构建 whisper-lib（首次需要 10-20 分钟）...
echo 请耐心等待，这是正常的...
call gradlew :whisper-lib:build
if errorlevel 1 (
    echo ❌ whisper-lib 构建失败
    echo 请查看错误信息，或参考 WHISPER_BUILD_NOW.md
    pause
    exit /b 1
)
echo ✅ whisper-lib 构建完成
echo.

echo [4/4] 构建 APK...
call gradlew :app:assembleDebug
if errorlevel 1 (
    echo ❌ APK 构建失败
    pause
    exit /b 1
)
echo ✅ APK 构建完成
echo.

echo ========================================
echo 🎉 构建成功！
echo ========================================
echo.
echo APK 位置：app\build\outputs\apk\debug\app-debug.apk
echo.
echo 安装命令：
echo adb install app\build\outputs\apk\debug\app-debug.apk
echo.
pause
