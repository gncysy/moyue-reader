import {
  app,
  BrowserWindow,
  ipcMain,
  session,
  protocol,
  nativeTheme,
  crashReporter,
  autoUpdater
} from 'electron'
import path from 'path'
import { ChildProcess, spawn } from 'child_process'
import fs from 'fs'
import { fileURLToPath } from 'url'
import log from 'electron-log'
 
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
 
// 扩展 App 类型
declare module 'electron' {
  interface App {
    isQuitting?: boolean
    backendPort?: number
  }
}
 
// 配置日志
log.transports.file.level = 'debug'
log.transports.file.maxSize = 5 * 1024 * 1024 // 5MB
log.transports.console.level = process.env.NODE_ENV === 'development' ? 'debug' : 'info'
 
// 崩溃报告
if (process.env.NODE_ENV !== 'development') {
  crashReporter.start({
    productName: '墨阅',
    companyName: '墨阅团队',
    submitURL: 'https://your-crash-server.com/api/crash',
    uploadToServer: false,
    compress: true
  })
}
 
// 窗口状态
interface WindowState {
  x?: number
  y?: number
  width: number
  height: number
  isMaximized?: boolean
}
 
const statePath = path.join(app.getPath('userData'), 'window-state.json')
let mainWindow: BrowserWindow | null = null
let javaProcess: ChildProcess | null = null
let javaReady = false
let javaRestartCount = 0
const MAX_RESTART_ATTEMPTS = 5
 
// 加载窗口状态
function loadWindowState(): WindowState {
  try {
    if (fs.existsSync(statePath)) {
      const data = fs.readFileSync(statePath, 'utf-8')
      return JSON.parse(data)
    }
  } catch (error) {
    log.warn('加载窗口状态失败:', error)
  }
  return { width: 1200, height: 800 }
}
 
// 保存窗口状态
function saveWindowState(win: BrowserWindow) {
  const bounds = win.getBounds()
  const state: WindowState = {
    x: bounds.x,
    y: bounds.y,
    width: bounds.width,
    height: bounds.height,
    isMaximized: win.isMaximized()
  }
  
  try {
    fs.writeFileSync(statePath, JSON.stringify(state, null, 2))
  } catch (error) {
    log.warn('保存窗口状态失败:', error)
  }
}
 
// 创建窗口
function createWindow() {
  const savedState = loadWindowState()
  
  mainWindow = new BrowserWindow({
    width: savedState.width,
    height: savedState.height,
    x: savedState.x,
    y: savedState.y,
    minWidth: 1000,
    minHeight: 600,
    frame: false,
    titleBarStyle: 'hidden',
    titleBarOverlay: {
      color: '#ffffff',
      symbolColor: '#000000',
      height: 32
    },
    show: false,
    backgroundColor: '#ffffff',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      preload: path.join(__dirname, 'preload.js'),
      webSecurity: true,
      allowRunningInsecureContent: false
    }
  })
 
  // 窗口事件
  mainWindow.on('maximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', true)
  })
 
  mainWindow.on('unmaximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', false)
  })
 
  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
    mainWindow?.focus()
    
    // 开发模式打开 DevTools
    if (process.env.NODE_ENV === 'development') {
      mainWindow?.webContents.openDevTools()
    }
  })
 
  mainWindow.on('close', () => {
    if (mainWindow) {
      if (!mainWindow.isMaximized()) {
        saveWindowState(mainWindow)
      }
    }
  })
 
  mainWindow.on('closed', () => {
    mainWindow = null
  })
 
  // 内容安全策略
  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow?.webContents.executeJavaScript(`
      if (!window.electronLog) {
        window.electronLog = {
          log: (...args) => console.log('[Renderer]', ...args),
          error: (...args) => console.error('[Renderer Error]', ...args)
        };
      }
    `)
  })
 
  // 拦截新窗口
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    require('electron').shell.openExternal(url)
    return { action: 'deny' }
  })
 
  // 加载页面
  const devUrl = process.env.VITE_DEV_SERVER_URL
  if (devUrl) {
    mainWindow.loadURL(devUrl).catch((err) => {
      log.error('加载开发服务器失败:', err)
      mainWindow?.loadFile(path.join(__dirname, '../dist/index.html'))
    })
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}
 
// 检查 Java 后端健康
function checkBackendHealth(): Promise<boolean> {
  return new Promise((resolve) => {
    if (!app.backendPort) {
      resolve(false)
      return
    }
    
    const controller = new AbortController()
    const timeout = setTimeout(() => {
      controller.abort()
      resolve(false)
    }, 5000)
    
    fetch(`http://localhost:${app.backendPort}/actuator/health`, {
      signal: controller.signal
    })
      .then((res) => {
        clearTimeout(timeout)
        resolve(res.ok)
      })
      .catch(() => {
        clearTimeout(timeout)
        resolve(false)
      })
  })
}
 
// 启动 Java 后端
function startJavaBackend(): Promise<boolean> {
  return new Promise((resolve) => {
    if (javaRestartCount >= MAX_RESTART_ATTEMPTS) {
      log.error('Java 后端启动失败次数过多，停止重试')
      mainWindow?.webContents.send('backend-startup-failed', {
        error: '后端启动失败次数过多，请检查日志'
      })
      resolve(false)
      return
    }
 
    const isDev = process.env.NODE_ENV === 'development'
    const jarPath = isDev
      ? path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
      : path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')
 
    // 检查 JAR 文件
    if (!fs.existsSync(jarPath)) {
      log.error('JAR 文件不存在:', jarPath)
      mainWindow?.webContents.send('backend-startup-failed', {
        error: '后端文件不存在，请重新安装'
      })
      resolve(false)
      return
    }
 
    // JVM 参数
    const jvmArgs = [
      '-Xshare:on',
      '-server',
      '-Xms256m',
      '-Xmx1024m',
      '-XX:+UseG1GC',
      '-XX:+UseStringDeduplication',
      '-XX:MaxGCPauseMillis=100',
      '-XX:InitiatingHeapOccupancyPercent=45',
      '-Djava.awt.headless=true',
      '-Dfile.encoding=UTF-8',
      `-Dserver.port=${process.env.BACKEND_PORT || 18080}`,
      '-jar',
      jarPath
    ]
 
    if (!isDev) {
      jvmArgs.unshift('-noverify', '-XX:TieredStopAtLevel=1')
    }
 
    log.info('启动 Java 后端:', jarPath)
    log.info('JVM 参数:', jvmArgs.join(' '))
 
    const spawnOptions: any = {
      stdio: 'pipe',
      detached: false,
      windowsHide: true,
      env: {
        ...process.env,
        JAVA_OPTS: isDev ? '' : '-XX:+UseContainerSupport'
      }
    }
 
    if (process.platform === 'win32') {
      spawnOptions.windowsHide = true
      spawnOptions.shell = false
    }
 
    try {
      javaProcess = spawn('java', jvmArgs, spawnOptions)
      javaRestartCount++
 
      // 日志输出
      if (isDev) {
        javaProcess.stdout?.on('data', (data: Buffer) => {
          const output = data.toString().trim()
          log.info(`[Java] ${output}`)
          
          // 检测启动完成
          if (output.includes('Started MoyueApplication') || output.includes('Tomcat started')) {
            javaReady = true
            javaRestartCount = 0
            app.backendPort = parseInt(process.env.BACKEND_PORT || '18080')
            log.info('Java 后端启动完成，端口:', app.backendPort)
            mainWindow?.webContents.send('backend-ready', { port: app.backendPort })
          }
        })
 
        javaProcess.stderr?.on('data', (data: Buffer) => {
          log.error(`[Java Error] ${data.toString().trim()}`)
        })
      }
 
      // 进程退出
      javaProcess.on('exit', (code: number | null, signal: string | null) => {
        log.info(`Java 进程退出，代码: ${code}, 信号: ${signal}`)
        javaProcess = null
        javaReady = false
 
        if (!app.isQuitting) {
          if (javaRestartCount < MAX_RESTART_ATTEMPTS) {
            log.info(`3秒后重启 Java 后端 (尝试 ${javaRestartCount + 1}/${MAX_RESTART_ATTEMPTS})`)
            setTimeout(startJavaBackend, 3000)
          } else {
            mainWindow?.webContents.send('backend-startup-failed', {
              error: '后端启动失败次数过多'
            })
          }
        }
      })
 
      javaProcess.on('error', (err: Error) => {
        log.error('Java 进程错误:', err)
        mainWindow?.webContents.send('backend-startup-failed', {
          error: err.message
        })
      })
 
      // 等待启动
      setTimeout(async () => {
        const healthy = await checkBackendHealth()
        if (healthy) {
          log.info('Java 后端健康检查通过')
          resolve(true)
        } else {
          log.warn('Java 后端健康检查失败，继续等待...')
        }
      }, 3000)
 
    } catch (err) {
      log.error('启动 Java 后端异常:', err)
      resolve(false)
    }
  })
}
 
// 停止 Java 后端
function stopJavaBackend(): Promise<void> {
  return new Promise((resolve) => {
    if (!javaProcess) {
      resolve()
      return
    }
 
    log.info('停止 Java 后端...')
    
    // 先尝试优雅关闭
    javaProcess.kill('SIGTERM')
    
    setTimeout(() => {
      if (javaProcess && !javaProcess.killed) {
        log.warn('强制终止 Java 后端')
        javaProcess.kill('SIGKILL')
      }
      javaProcess = null
      javaReady = false
      resolve()
    }, 5000)
  })
}
 
// IPC 处理
ipcMain.on('window-minimize', () => {
  mainWindow?.minimize()
})
 
ipcMain.handle('window-maximize', () => {
  if (!mainWindow) return { isMaximized: false }
  
  if (mainWindow.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow.maximize()
  }
  
  return { isMaximized: mainWindow.isMaximized() }
})
 
ipcMain.handle('window-close', () => {
  mainWindow?.close()
  return { closed: true }
})
 
ipcMain.handle('get-app-version', () => {
  return app.getVersion()
})
 
ipcMain.handle('get-backend-status', () => {
  return {
    running: javaProcess !== null,
    ready: javaReady,
    port: app.backendPort
  }
})
 
ipcMain.handle('restart-backend', async () => {
  await stopJavaBackend()
  javaRestartCount = 0
  return await startJavaBackend()
})
 
ipcMain.handle('get-native-theme', () => {
  return {
    shouldUseDarkColors: nativeTheme.shouldUseDarkColors
  }
})
 
ipcMain.on('native-theme-updated', () => {
  const shouldUseDarkColors = nativeTheme.shouldUseDarkColors
  mainWindow?.webContents.send('theme-changed', { shouldUseDarkColors })
})
 
// 自动更新
function setupAutoUpdater() {
  if (process.env.NODE_ENV === 'development') return
 
  autoUpdater.setFeedURL({
    provider: 'github',
    owner: 'gncysy',
    repo: 'moyue-reader'
  })
 
  autoUpdater.on('checking-for-update', () => {
    log.info('检查更新中...')
  })
 
  autoUpdater.on('update-available', (info) => {
    log.info('发现新版本:', info.version)
    mainWindow?.webContents.send('update-available', info)
  })
 
  autoUpdater.on('update-not-available', () => {
    log.info('当前已是最新版本')
  })
 
  autoUpdater.on('download-progress', (progress) => {
    mainWindow?.webContents.send('update-download-progress', progress)
  })
 
  autoUpdater.on('update-downloaded', (info) => {
    log.info('更新下载完成:', info.version)
    mainWindow?.webContents.send('update-downloaded', info)
  })
 
  autoUpdater.on('error', (err) => {
    log.error('自动更新错误:', err)
  })
}
 
// 注册自定义协议
function registerProtocols() {
  protocol.registerSchemesAsPrivileged([
    {
      scheme: 'moyue',
      privileges: {
        secure: true,
        standard: true,
        supportFetchAPI: true
      }
    }
  ])
}
 
// 应用启动
app.whenReady().then(async () => {
  registerProtocols()
  setupAutoUpdater()
  
  // 配置会话
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        ...details.responseHeaders,
        'Content-Security-Policy': [
          "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' ws: wss:;"
        ]
      }
    })
  })
 
  // 启动后端
  await startJavaBackend()
  
  // 创建窗口
  createWindow()
  
  // 检查更新
  if (process.env.NODE_ENV !== 'development') {
    setTimeout(() => autoUpdater.checkForUpdates(), 30000)
  }
})
 
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
 
app.on('before-quit', async () => {
  app.isQuitting = true
  await stopJavaBackend()
})
 
app.on('activate', () => {
  if (mainWindow === null) {
    createWindow()
  }
})
 
app.on('web-contents-created', (event, contents) => {
  contents.on('will-navigate', (event, navigationUrl) => {
    const parsedUrl = new URL(navigationUrl)
    
    if (parsedUrl.origin !== 'http://localhost:5173' && parsedUrl.protocol !== 'file:') {
      event.preventDefault()
    }
  })
})
