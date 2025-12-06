@echo off
REM 修复Google下载 - 添加hosts记录
REM 需要管理员权限运行

echo ========================================
echo Google Hosts 修复工具
echo ========================================
echo.
echo 此脚本将添加Google镜像到hosts文件
echo 需要管理员权限
echo.
pause

REM 检查管理员权限
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [错误] 需要管理员权限！
    echo 请右键点击此文件，选择"以管理员身份运行"
    pause
    exit /b 1
)

echo 正在备份hosts文件...
copy C:\Windows\System32\drivers\etc\hosts C:\Windows\System32\drivers\etc\hosts.backup.%date:~0,4%%date:~5,2%%date:~8,2%

echo.
echo 正在添加Google镜像记录...
echo.

REM 添加hosts记录
echo # Google Android SDK Mirror >> C:\Windows\System32\drivers\etc\hosts
echo 203.208.40.33 dl.google.com >> C:\Windows\System32\drivers\etc\hosts
echo 203.208.40.33 dl-ssl.google.com >> C:\Windows\System32\drivers\etc\hosts

echo ========================================
echo 完成！
echo ========================================
echo.
echo 已添加以下记录到hosts文件:
echo   203.208.40.33 dl.google.com
echo   203.208.40.33 dl-ssl.google.com
echo.
echo 备份文件位置:
echo   C:\Windows\System32\drivers\etc\hosts.backup.*
echo.
echo 现在可以在Android Studio中下载SDK了
echo.
pause
