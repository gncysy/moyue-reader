import { app, BrowserWindow } from 'electron'
import path from 'path'
import { spawn } from 'child_process'

let mainWindow: BrowserWindow | null = null
let javaProcess: any = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  })

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  let javaPath = 'java'
  let jarPath = ''

  if (!isDev) {
    const jrePath = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
    if (require('fs').existsSync(jrePath)) {
      javaPath = jrePath
    }
    jarPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')
  } else {
    jarPath = path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
  }

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

  javaProcess.on('exit', (code: number) => {
    console.log(`Java 进程退出，代码: ${code}`)
    if (!app.isQuitting) {
      setTimeout(startJavaBackend, 3000)
    }
  })
}

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
