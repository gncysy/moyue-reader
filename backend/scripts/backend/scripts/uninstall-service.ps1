# uninstall-service.ps1
$ServiceName = "MoyueBackend"
$ServiceDir = "C:\Program Files\Moyue\backend"

Write-Host "🔧 停止服务..." -ForegroundColor Yellow

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($service -and $service.Status -eq 'Running') {
    Stop-Service $ServiceName -Force
}

if (Test-Path "$ServiceDir\moyue-service.exe") {
    Set-Location $ServiceDir
    Start-Process -FilePath "$ServiceDir\moyue-service.exe" -ArgumentList "uninstall" -Wait -NoNewWindow
}

Write-Host "✅ 服务已卸载" -ForegroundColor Green
