# uninstall-service.ps1
param(
    [switch]$CleanFiles = $false
)

$ErrorActionPreference = "Stop"

$ServiceName = "MoyueBackend"
$ServiceDir = "C:\Program Files\Moyue\backend"

Write-Host "🔧 开始卸载服务..." -ForegroundColor Yellow

# 检查服务是否存在
$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Host "⚠️  服务不存在" -ForegroundColor Yellow
    
    if ($CleanFiles -and (Test-Path $ServiceDir)) {
        Write-Host "`n🗑️  服务不存在，是否删除安装目录和文件？" -ForegroundColor Yellow
        $confirm = Read-Host "确认删除? (y/N)"
        if ($confirm -eq 'y' -or $confirm -eq 'Y') {
            Remove-Item $ServiceDir -Recurse -Force
            Write-Host "✅ 已删除目录: $ServiceDir" -ForegroundColor Green
        }
    }
    exit 0
}

# 停止服务
if ($service.Status -eq 'Running') {
    Write-Host "🛑 停止服务..." -ForegroundColor Yellow
    try {
        Stop-Service $ServiceName -Force -ErrorAction Stop
        Write-Host "✅ 服务已停止" -ForegroundColor Green
    } catch {
        Write-Host "❌ 停止服务失败: $_" -ForegroundColor Red
        exit 1
    }
}

# 卸载服务
if (Test-Path "$ServiceDir\moyue-service.exe") {
    try {
        Set-Location $ServiceDir
        Start-Process -FilePath "$ServiceDir\moyue-service.exe" -ArgumentList "uninstall" -Wait -NoNewWindow -RedirectStandardOutput "$ServiceDir\uninstall.log" -RedirectStandardError "$ServiceDir\uninstall-error.log"
        Write-Host "✅ 服务已卸载" -ForegroundColor Green
    } catch {
        Write-Host "❌ 卸载服务失败: $_" -ForegroundColor Red
        Write-Host "   查看 $ServiceDir\uninstall-error.log 获取详情" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "⚠️  未找到 WinSW 可执行文件" -ForegroundColor Yellow
}

# 清理文件
if ($CleanFiles) {
    Write-Host "`n🗑️  是否删除安装目录和文件?" -ForegroundColor Yellow
    Write-Host "   包括: $ServiceDir" -ForegroundColor White
    Write-Host "   警告: 此操作不可撤销！" -ForegroundColor Red
    $confirm = Read-Host "确认删除? (y/N)"
    
    if ($confirm -eq 'y' -or $confirm -eq 'Y') {
        try {
            Remove-Item $ServiceDir -Recurse -Force
            Write-Host "✅ 已删除目录: $ServiceDir" -ForegroundColor Green
        } catch {
            Write-Host "❌ 删除目录失败: $_" -ForegroundColor Red
            Write-Host "   请手动删除: $ServiceDir" -ForegroundColor Yellow
        }
    } else {
        Write-Host "ℹ️  保留文件目录: $ServiceDir" -ForegroundColor Cyan
    }
}

Write-Host "`n✅ 卸载完成！" -ForegroundColor Green
