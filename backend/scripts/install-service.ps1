<#
.SYNOPSIS
    安装 Moyue Reader 后端服务
.DESCRIPTION
    将 moyue-backend.jar 注册为 Windows 服务
.NOTES
    需要管理员权限运行
#>
 
# 错误处理
$ErrorActionPreference = "Stop"
 
# 配置变量
$serviceName = "MoyueBackend"
$displayName = "Moyue Reader Backend Service"
$description = "Moyue Reader 后端服务 - Spring Boot 应用"
$jarPath = "build\libs\moyue-backend.jar"
$logPath = "C:\ProgramData\Moyue\logs"
$configPath = "C:\ProgramData\Moyue\config"
 
# 检查管理员权限
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Error "此脚本需要管理员权限运行"
    exit 1
}
 
# 检查 JAR 文件是否存在
if (-not (Test-Path $jarPath)) {
    Write-Error "找不到 JAR 文件: $jarPath"
    Write-Host "请先运行 ./gradlew build 构建 JAR 文件"
    exit 1
}
 
# 创建必要的目录
New-Item -ItemType Directory -Force -Path $logPath | Out-Null
New-Item -ItemType Directory -Force -Path $configPath | Out-Null
 
# 复制配置文件（如果存在）
if (Test-Path "application.yml") {
    Copy-Item "application.yml" -Destination "$configPath\application.yml" -Force
}
 
Write-Host "正在安装服务..." -ForegroundColor Green
 
# 使用 NSSM 安装服务（如果已安装 NSSM）
$nssmPath = "nssm.exe"
if (Get-Command $nssmPath -ErrorAction SilentlyContinue) {
    & $nssmPath install $serviceName "java.exe" `
        -jar $jarPath `
        --spring.config.location="$configPath\application.yml" `
        --logging.file.name="$logPath\moyue-backend.log"
    
    & $nssmPath set $serviceName DisplayName $displayName
    & $nssmPath set $serviceName Description $description
    & $nssmPath set $serviceName AppDirectory (Get-Location).Path
    & $nssmPath set $serviceName AppStdout "$logPath\stdout.log"
    & $nssmPath set $serviceName AppStderr "$logPath\stderr.log"
    
    Write-Host "服务安装成功（使用 NSSM）" -ForegroundColor Green
}
else {
    # 使用 sc.exe 安装服务
    $javaPath = (Get-Command java.exe).Source
    
    & sc.exe create $serviceName binPath= "`"$javaPath`" -jar `"$jarPath`" --spring.config.location=`"$configPath\application.yml`"" DisplayName= $displayName start= auto
    & sc.exe description $serviceName $description
    
    Write-Host "服务安装成功（使用 sc.exe）" -ForegroundColor Green
    Write-Warning "推荐安装 NSSM 以获得更好的日志管理"
}
 
# 启动服务
Write-Host "正在启动服务..." -ForegroundColor Yellow
& sc.exe start $serviceName
 
Write-Host "`n服务安装并启动完成！" -ForegroundColor Green
Write-Host "服务名称: $serviceName"
Write-Host "日志路径: $logPath"
Write-Host "配置路径: $configPath"
