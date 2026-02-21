# install-service.ps1
param(
    [string]$JarPath = "",
    [string]$JavaPath = "",
    [string]$InstallPath = "C:\Program Files\Moyue\backend",
    [switch]$Force = $false
)

$ErrorActionPreference = "Stop"

# 设置默认 JAR 路径
if ($JarPath -eq "") {
    $JarPath = "$PSScriptRoot\moyue-backend.jar"
}
if ($JavaPath -eq "") {
    $JavaPath = "javaw.exe"
}

# 验证 JAR 文件存在
if (-not (Test-Path $JarPath)) {
    Write-Host "❌ 未找到 JAR 文件: $JarPath" -ForegroundColor Red
    exit 1
}

# WinSW 配置
$WinSWVersion = "v2.12.0"
$WinSWUrl = "https://github.com/winsw/winsw/releases/download/$WinSWVersion/WinSW-x64.exe"
$WinSWHash = "A5D6F8A1B3C4E5D6A7B8C9D0E1F2A3B4C5D6E7F8A9B0C1D2E3F4A5B6C7D8E9F0" # 替换为实际的 SHA256

# 检查服务是否已存在
$service = Get-Service -Name "MoyueBackend" -ErrorAction SilentlyContinue
if ($service -and -not $Force) {
    Write-Host "⚠️  服务已存在，使用 -Force 参数强制重新安装" -ForegroundColor Yellow
    exit 0
}

# 如果服务存在且指定了 -Force，先卸载
if ($service -and $Force) {
    Write-Host "🔄 正在卸载现有服务..." -ForegroundColor Yellow
    & "$PSScriptRoot\uninstall-service.ps1"
}

Write-Host "📦 安装 Windows 服务..." -ForegroundColor Cyan

# 创建服务目录
try {
    New-Item -ItemType Directory -Force -Path $InstallPath | Out-Null
    Write-Host "✅ 创建目录: $InstallPath" -ForegroundColor Green
} catch {
    Write-Host "❌ 创建目录失败: $_" -ForegroundColor Red
    exit 1
}

# 创建日志目录
try {
    New-Item -ItemType Directory -Force -Path "$InstallPath\logs" | Out-Null
    Write-Host "✅ 创建日志目录" -ForegroundColor Green
} catch {
    Write-Host "❌ 创建日志目录失败: $_" -ForegroundColor Red
    exit 1
}

# 复制 JAR 文件
try {
    Copy-Item $JarPath "$InstallPath\moyue-backend.jar" -Force
    Write-Host "✅ 复制 JAR 文件" -ForegroundColor Green
} catch {
    Write-Host "❌ 复制 JAR 文件失败: $_" -ForegroundColor Red
    exit 1
}

# 下载 WinSW
$WinSWPath = "$InstallPath\moyue-service.exe"
if (-not (Test-Path $WinSWPath)) {
    Write-Host "📥 下载 WinSW $WinSWVersion..." -ForegroundColor Yellow
    
    # 下载文件
    try {
        Invoke-WebRequest -Uri $WinSWUrl -OutFile $WinSWPath
        Write-Host "✅ WinSW 下载完成" -ForegroundColor Green
        
        # 验证哈希
        $actualHash = (Get-FileHash -Path $WinSWPath -Algorithm SHA256).Hash.ToUpper()
        if ($actualHash -ne $WinSWHash) {
            Write-Host "❌ WinSW 哈希校验失败！" -ForegroundColor Red
            Write-Host "   期望: $WinSWHash" -ForegroundColor Red
            Write-Host "   实际: $actualHash" -ForegroundColor Red
            Remove-Item $WinSWPath -Force
            exit 1
        }
        Write-Host "✅ WinSW 哈希校验通过" -ForegroundColor Green
    } catch {
        Write-Host "❌ 下载 WinSW 失败: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ WinSW 已存在" -ForegroundColor Green
}

# 创建服务配置
$ServiceXml = @"
<service>
  <id>MoyueBackend</id>
  <name>墨阅后端服务</name>
  <description>墨阅阅读器后端服务</description>
  <executable>$JavaPath</executable>
  <arguments>-Xshare:auto -jar "$InstallPath\moyue-backend.jar" --server.port=0 --spring.profiles.active=prod</arguments>
  <log mode="roll"></log>
  <logpath>$InstallPath\logs</logpath>
  <delayedAutoStart>true</delayedAutoStart>
  <onfailure action="restart" delay="10 sec"/>
</service>
"@

try {
    $ServiceXml | Out-File -FilePath "$InstallPath\moyue-service.xml" -Encoding UTF8
    Write-Host "✅ 生成服务配置文件" -ForegroundColor Green
} catch {
    Write-Host "❌ 生成配置文件失败: $_" -ForegroundColor Red
    exit 1
}

# 安装服务
try {
    Set-Location $InstallPath
    Start-Process -FilePath "$WinSWPath" -ArgumentList "install" -Wait -NoNewWindow -RedirectStandardOutput "$InstallPath\install.log" -RedirectStandardError "$InstallPath\install-error.log"
    Write-Host "✅ 服务安装成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 服务安装失败: $_" -ForegroundColor Red
    Write-Host "   查看 $InstallPath\install-error.log 获取详情" -ForegroundColor Yellow
    exit 1
}

# 启动服务
try {
    Start-Process -FilePath "$WinSWPath" -ArgumentList "start" -Wait -NoNewWindow
    Write-Host "✅ 服务启动成功" -ForegroundColor Green
} catch {
    Write-Host "❌ 服务启动失败: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n🎉 服务安装完成！" -ForegroundColor Green
Write-Host "   服务名称: MoyueBackend" -ForegroundColor Cyan
Write-Host "   安装路径: $InstallPath" -ForegroundColor Cyan
Write-Host "   日志路径: $InstallPath\logs" -ForegroundColor Cyan
Write-Host "`n   使用以下命令管理服务:" -ForegroundColor Yellow
Write-Host "   停止:   & '$WinSWPath' stop" -ForegroundColor White
Write-Host "   重启:   & '$WinSWPath' restart" -ForegroundColor White
Write-Host "   卸载:   & '$PSScriptRoot\uninstall-service.ps1'" -ForegroundColor White
