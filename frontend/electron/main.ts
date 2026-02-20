import { app, BrowserWindow, ipcMain } from 'electron'
import path from 'path'
import { spawn } from 'child_process'
import fs from 'fs'

let mainWindow: BrowserWindow | null = null
let javaProcess: any = null
let isQuitting = false

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 1000,
    minHeight: 600,
    frame: false,
    titleBarStyle: 'hidden',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    },
    icon: path.join(__dirname, '../build/icon.ico')
  })

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
  })

  mainWindow.on('closed', () => {
    if (!isQuitting) {
      app.quit()
    }
  })
}

// IPC 处理
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

ipcMain.handle('get-app-path', () => {
  return app.getPath('userData')
})

ipcMain.handle('open-external', (event, url) => {
  require('electron').shell.openExternal(url)
})

ipcMain.handle('open-path', (event, path) => {
  require('electron').shell.openPath(path)
})

// 监听窗口状态变化
function setupWindowListeners() {
  if (!mainWindow) return
  
  mainWindow.on('maximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', true)
  })

  mainWindow.on('unmaximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', false)
  })
}

// 启动 Java 后端
function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  
  let javaPath = 'java'
  let jarPath = ''
  
  if (!isDev) {
    // 生产环境：使用打包的 JRE
    const jrePath = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
    if (fs.existsSync(jrePath)) {
      javaPath = jrePath
    }
    jarPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')
  } else {
    // 开发环境：使用系统 Java
    jarPath = path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
  }

  console.log('启动 Java 后端:', javaPath, jarPath)

  javaProcess = spawn(javaPath, ['-jar', jarPath], {
    stdio: 'pipe',
    detached: false
  })

  javaProcess.stdout?.on('data', (data: Buffer) => {
    console.log(`[Java] ${data.toString().trim()}`)
  })

  javaProcess.stderr?.on('data', (data: Buffer) => {
    console.error(`[Java Error] ${data.toString().trim()}`)
  })

  javaProcess.on('error', (err) => {
    console.error('启动 Java 失败:', err)
  })

  javaProcess.on('exit', (code: number) => {
    console.log(`Java 进程退出，代码: ${code}`)
    if (!isQuitting && code !== 0) {
      console.log('Java 进程异常退出，3秒后重启...')
      setTimeout(startJavaBackend, 3000)
    }
  })
}

app.whenReady().then(() => {
  startJavaBackend()
  createWindow()
  setupWindowListeners()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('before-quit', () => {
  isQuitting = true
  if (javaProcess && !javaProcess.killed) {
    javaProcess.kill()
  }
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
