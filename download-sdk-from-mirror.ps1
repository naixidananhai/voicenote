# Android SDK 国内镜像下载脚本
# 从腾讯云镜像下载SDK组件

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Android SDK 国内镜像下载工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# SDK路径
$sdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
Write-Host "SDK路径: $sdkRoot" -ForegroundColor Yellow

# 创建SDK目录
if (-not (Test-Path $sdkRoot)) {
    New-Item -ItemType Directory -Path $sdkRoot -Force | Out-Null
    Write-Host "已创建SDK目录" -ForegroundColor Green
}

# 镜像地址
$mirror = "https://mirrors.cloud.tencent.com/AndroidSDK"

# 需要下载的组件
$components = @(
    @{
        Name = "Command Line Tools (latest)"
        Url = "$mirror/commandlinetools-win-9477386_latest.zip"
        ExtractTo = "$sdkRoot\cmdline-tools"
        Rename = "latest"
    },
    @{
        Name = "Platform Tools"
        Url = "$mirror/platform-tools_r34.0.5-windows.zip"
        ExtractTo = "$sdkRoot"
    },
    @{
        Name = "Build Tools 34.0.0"
        Url = "$mirror/build-tools_r34-windows.zip"
        ExtractTo = "$sdkRoot\build-tools"
        Rename = "34.0.0"
    }
)

# 下载函数
function Download-Component {
    param($Component)
    
    Write-Host ""
    Write-Host "----------------------------------------" -ForegroundColor Cyan
    Write-Host "下载: $($Component.Name)" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Cyan
    
    $zipFile = "$env:TEMP\$($Component.Name -replace ' ', '_').zip"
    
    try {
        Write-Host "正在下载..." -ForegroundColor Gray
        Write-Host "URL: $($Component.Url)" -ForegroundColor Gray
        
        # 使用Invoke-WebRequest下载
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $Component.Url -OutFile $zipFile -UseBasicParsing
        
        Write-Host "✓ 下载完成" -ForegroundColor Green
        
        # 解压
        Write-Host "正在解压..." -ForegroundColor Gray
        $extractPath = $Component.ExtractTo
        
        if (-not (Test-Path $extractPath)) {
            New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
        }
        
        Expand-Archive -Path $zipFile -DestinationPath $extractPath -Force
        
        # 重命名（如果需要）
        if ($Component.Rename) {
            $extractedFolder = Get-ChildItem -Path $extractPath -Directory | Select-Object -First 1
            if ($extractedFolder) {
                $targetPath = Join-Path $extractPath $Component.Rename
                if (Test-Path $targetPath) {
                    Remove-Item -Path $targetPath -Recurse -Force
                }
                Move-Item -Path $extractedFolder.FullName -Destination $targetPath -Force
            }
        }
        
        Write-Host "✓ 解压完成" -ForegroundColor Green
        
        # 清理
        Remove-Item -Path $zipFile -Force
        
    } catch {
        Write-Host "✗ 失败: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "请手动下载:" -ForegroundColor Yellow
        Write-Host $Component.Url -ForegroundColor Cyan
        return $false
    }
    
    return $true
}

# 主流程
Write-Host ""
Write-Host "准备下载 $($components.Count) 个组件..." -ForegroundColor Yellow
Write-Host ""

$successCount = 0
foreach ($component in $components) {
    if (Download-Component -Component $component) {
        $successCount++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "下载完成: $successCount / $($components.Count)" -ForegroundColor $(if ($successCount -eq $components.Count) { "Green" } else { "Yellow" })
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($successCount -eq $components.Count) {
    Write-Host "✓ 所有组件已成功安装到:" -ForegroundColor Green
    Write-Host "  $sdkRoot" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "现在可以在项目中使用这些SDK组件了！" -ForegroundColor Green
} else {
    Write-Host "⚠ 部分组件下载失败" -ForegroundColor Yellow
    Write-Host "请检查网络连接或手动下载" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
