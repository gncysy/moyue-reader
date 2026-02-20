# install-service.ps1
param(
    [string]$JarPath = "",
    [string]$JavaPath = ""
)

$ErrorActionPreference = "Stop"

if ($JarPath -eq "") {
    $JarPath = "$PSScriptRoot\moyue-backend.jar"
}
if ($JavaPath -eq "") {
    $JavaPath = "javaw.exe"
}

if (-not (Test-Path $JarPath)) {
    Write-Host "❌ 未找到 JAR 文件: $JarPath" -ForegroundColor Red
    exit 1
}

# 检查服务是否已存在
$service = Get-Service -Name "MoyueBackend" -ErrorAction SilentlyContinue
if ($service) {
    Write-Host "✅ 服务已存在" -ForegroundColor Green
    exit 0
}

Write-Host "📦 安装 Windows 服务..." -ForegroundColor Cyan

# 创建服务目录
$ServiceDir = "C:\Program Files\Moyue\backend"
New-Item -ItemType Directory -Force -Path $ServiceDir | Out-Null

# 复制 JAR 文件
Copy-Item $JarPath "$ServiceDir\moyue-backend.jar" -Force

# 下载 WinSW
$WinSWPath = "$ServiceDir\moyue-service.exe"
if (-not (Test-Path $WinSWPath)) {
    Write-Host "📥 下载 WinSW..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe" -OutFile $WinSWPath
}

# 创建服务配置
$ServiceXml = @"
<service>
  <id>MoyueBackend</id>
  <name>墨阅后端服务</name>
  <description>墨阅阅读器后端服务</description>
  <executable>$JavaPath</executable>
  <arguments>-Xshare:auto -jar "$ServiceDir\moyue-backend.jar" --server.port=0 --spring.profiles.active=prod</arguments>
  <log mode="roll"></log>
  <logpath>$ServiceDir\logs</logpath>
  <delayedAutoStart>true</delayedAutoStart>
  <onfailure action="restart" delay="10 sec"/>
</service>
"@

$ServiceXml | Out-File -FilePath "$ServiceDir\moyue-service.xml" -Encoding UTF8

# 安装并启动服务
Set-Location $ServiceDir
Start-Process -FilePath "$ServiceDir\moyue-service.exe" -ArgumentList "install" -Wait -NoNewWindow
Start-Process -FilePath "$ServiceDir\moyue-service.exe" -ArgumentList "start" -Wait -NoNewWindow

Write-Host "✅ 服务安装完成！" -ForegroundColor Green
