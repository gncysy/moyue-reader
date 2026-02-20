import { app, BrowserWindow, ipcMain } from 'electron'  // 修复：添加 ipcMain
import path from 'path'
import { spawn } from 'child_process'

let mainWindow: BrowserWindow | null = null
let javaProcess: any = null

// 声明 app.isQuitting
declare module 'electron' {
  interface App {
    isQuitting?: boolean
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 1000,
    minHeight: 600,
    frame: false,
    titleBarStyle: 'hidden',
    show: false,  // 先不显示，等 ready-to-show
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    icon: path.join(__dirname, '../build/icon.ico')
  })

  // 修复：窗口事件监听移到 createWindow 内部
  mainWindow.on('maximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', true)
  })

  mainWindow.on('unmaximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', false)
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
    mainWindow?.focus()
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })

  // 加载页面
  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

// 启动 Java 后端（无黑框版本）
function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  const jarPath = isDev
    ? path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
    : path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')

  // JVM 参数（包含 CDS 加速）
  const jvmArgs = [
    '-Xshare:on',                    // 启用 CDS
    '-server',
    '-Xms128m',
    '-Xmx512m',
    '-XX:+UseG1GC',
    '-XX:+UseStringDeduplication',
    '-XX:MaxGCPauseMillis=100',
    '-Djava.awt.headless=true',
    '-jar', jarPath
  ]

  if (!isDev) {
    jvmArgs.push('-noverify')
    jvmArgs.push('-XX:TieredStopAtLevel=1')
  }

  // 无黑框启动配置
  const spawnOptions: any = {
    stdio: 'pipe',
    detached: false,
    windowsHide: true  // Windows 关键：隐藏控制台
  }

  if (process.platform === 'win32') {
    spawnOptions.windowsHide = true
    spawnOptions.shell = false
  }

  javaProcess = spawn('java', jvmArgs, spawnOptions)

  if (isDev) {
    javaProcess.stdout?.on('data', (data: Buffer) => {
      console.log(`[Java] ${data.toString().trim()}`)
    })
    javaProcess.stderr?.on('data', (data: Buffer) => {
      console.error(`[Java Error] ${data.toString().trim()}`)
    })
  }

  javaProcess.on('exit', (code: number) => {
    console.log(`Java 进程退出，代码: ${code}`)
    if (!app.isQuitting) {
      setTimeout(startJavaBackend, 3000)
    }
  })

  javaProcess.on('error', (err: Error) => {
    console.error('Java 进程启动失败:', err)
  })
}

// IPC 处理（移到外部）
ipcMain.on('window-minimize', () => {
  mainWindow?.minimize()
})

ipcMain.handle('window-maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow?.maximize()
  }
  return { isMaximized: mainWindow?.isMaximized() ?? false }
})

ipcMain.on('window-close', () => {
  mainWindow?.close()
})

// 应用生命周期
app.whenReady().then(() => {
  startJavaBackend()
  setTimeout(createWindow, 1000)
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  app.isQuitting = true
  if (javaProcess && !javaProcess.killed) {
    javaProcess.kill()
  }
})

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow()
  }
})
