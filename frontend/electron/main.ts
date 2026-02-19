import { app, BrowserWindow } from 'electron'
import path from 'path'
import { spawn } from 'child_process'

let mainWindow: BrowserWindow | null = null
let javaProcess: any = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 1000,
    minHeight: 600,
    frame: false, // 👈 关键：移除系统标题栏
    titleBarStyle: 'hidden', // MacOS 也隐藏
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    icon: path.join(__dirname, '../build/icon.ico')
  })

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
    // 开发环境可以打开 devtools
    // mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
  })
}

// 启动 Java 后端
function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  const jarPath = isDev
    ? path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
    : path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')

  javaProcess = spawn('java', ['-jar', jarPath], {
    stdio: 'pipe',
    detached: false
  })

  javaProcess.stdout?.on('data', (data: Buffer) => {
    console.log(`[Java] ${data.toString().trim()}`)
  })

  javaProcess.stderr?.on('data', (data: Buffer) => {
    console.error(`[Java Error] ${data.toString().trim()}`)
  })

  javaProcess.on('exit', (code: number) => {
    console.log(`Java 进程退出，代码: ${code}`)
    if (!app.isQuitting) {
      setTimeout(startJavaBackend, 3000)
    }
  })
}

// 在 app.whenReady() 之前添加
ipcMain.on('window-minimize', () => {
  mainWindow?.minimize()
})

ipcMain.handle('window-maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow?.maximize()
  }
  return { isMaximized: mainWindow?.isMaximized() }
})

ipcMain.on('window-close', () => {
  mainWindow?.close()
})

// 监听窗口状态变化
mainWindow?.on('maximize', () => {
  mainWindow?.webContents.send('window-maximized-changed', true)
})

mainWindow?.on('unmaximize', () => {
  mainWindow?.webContents.send('window-maximized-changed', false)
})

app.whenReady().then(() => {
  startJavaBackend()
  createWindow()
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
