<#
.SYNOPSIS
    卸载 Moyue Reader 后端服务
.DESCRIPTION
    从 Windows 系统中移除 moyue-backend 服务
.NOTES
    需要管理员权限运行
#>
 
# 错误处理
$ErrorActionPreference = "Stop"
 
# 配置变量
$serviceName = "MoyueBackend"
$logPath = "C:\ProgramData\Moyue\logs"
$configPath = "C:\ProgramData\Moyue\config"
 
# 检查管理员权限
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error "此脚本需要管理员权限运行"
    exit 1
}
 
# 检查服务是否存在
$service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Warning "服务不存在: $serviceName"
    exit 0
}
 
Write-Host "正在停止服务..." -ForegroundColor Yellow
& sc.exe stop $serviceName
 
Start-Sleep -Seconds 2
 
Write-Host "正在卸载服务..." -ForegroundColor Green
 
# 使用 NSSM 卸载（如果可用）
$nssmPath = "nssm.exe"
if (Get-Command $nssmPath -ErrorAction SilentlyContinue) {
    & $nssmPath remove $serviceName confirm
}
else {
    & sc.exe delete $serviceName
}
 
Write-Host "服务卸载成功" -ForegroundColor Green
 
# 询问是否删除数据
$removeData = Read-Host "是否删除日志和配置文件？(y/N)"
if ($removeData -eq "y" -or $removeData -eq "Y") {
    Write-Host "正在删除数据..." -ForegroundColor Yellow
    
    if (Test-Path $logPath) {
        Remove-Item $logPath -Recurse -Force
        Write-Host "已删除日志目录: $logPath"
    }
    
    if (Test-Path $configPath) {
        Remove-Item $configPath -Recurse -Force
        Write-Host "已删除配置目录: $configPath"
    }
    
    Write-Host "数据清理完成" -ForegroundColor Green
}
