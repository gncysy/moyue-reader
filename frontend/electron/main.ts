import { app, BrowserWindow, ipcMain, globalShortcut, Menu, shell, dialog } from 'electron'
import path from 'path'
import { spawn } from 'child_process'
import fs from 'fs'
import http from 'http'

let mainWindow: BrowserWindow | null = null
let javaProcess: any = null
let isQuitting = false

function createWindow() {
  const isDev = process.env.NODE_ENV === 'development'
  
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
      preload: path.join(__dirname, 'preload.js'),
      devTools: isDev
    },
    icon: path.join(__dirname, '../build/icon.ico')
  })

  mainWindow.removeMenu()

  // ✅ 生产环境禁用开发者工具
  if (!isDev) {
    mainWindow.webContents.on('devtools-opened', () => {
      mainWindow?.webContents.closeDevTools()
    })
    
    mainWindow.webContents.on('before-input-event', (event, input) => {
      if (input.key === 'F12' || 
          (input.control && input.shift && input.key === 'I') ||
          (input.meta && input.alt && input.key === 'I')) {
        event.preventDefault()
      }
    })
  }

  // 自定义右键菜单
  mainWindow.webContents.on('context-menu', (event, params) => {
    event.preventDefault()
    
    const menuTemplate: any[] = []
    
    if (params.selectionText && params.selectionText.trim().length > 0) {
      menuTemplate.push(
        {
          label: '📋 复制',
          accelerator: 'Ctrl+C',
          click: () => {
            mainWindow?.webContents.copy()
          }
        },
        { type: 'separator' }
      )
    }
    
    if (menuTemplate.length > 0) {
      const menu = Menu.buildFromTemplate(menuTemplate)
      menu.popup({
        window: mainWindow!,
        x: params.x,
        y: params.y
      })
    }
  })

  if (isDev) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL || 'http://localhost:5173')
    mainWindow.webContents.openDevTools()
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

// ✅ 启动 Java 后端（无窗口版）
function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  
  let javaPath = 'java'
  let jarPath = ''
  
  if (!isDev) {
    // 生产环境：使用 javaw.exe 无窗口
    const jrePath = path.join(process.resourcesPath, 'jre', 'bin', 'javaw.exe')
    if (fs.existsSync(jrePath)) {
      javaPath = jrePath
    } else {
      javaPath = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
    }
    jarPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')
    
    console.log('启动后端服务（无窗口模式）')
    
    javaProcess = spawn(javaPath, ['-jar', jarPath, '--server.port=0'], {
      detached: true,
      stdio: 'ignore',
      windowsHide: true
    })
    
    javaProcess.unref()
    
  } else {
    // 开发环境：正常显示
    jarPath = path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
    console.log('启动后端（开发模式）:', jarPath)
    
    javaProcess = spawn(javaPath, ['-jar', jarPath, '--server.port=0'], {
      stdio: 'pipe'
    })
    
    javaProcess.stdout?.on('data', (data: Buffer) => {
      console.log(`[Java] ${data.toString().trim()}`)
    })
    
    javaProcess.stderr?.on('data', (data: Buffer) => {
      console.error(`[Java Error] ${data.toString().trim()}`)
    })
  }

  javaProcess.on('error', (err) => {
    console.error('启动 Java 失败:', err)
    if (!isDev) {
      dialog.showErrorBox('启动失败', '无法启动后端服务：' + err.message)
    }
  })

  javaProcess.on('exit', (code: number) => {
    console.log(`Java 进程退出，代码: ${code}`)
    if (!isQuitting && code !== 0) {
      console.log('Java 进程异常退出，3秒后重启...')
      setTimeout(startJavaBackend, 3000)
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
  shell.openExternal(url)
})

ipcMain.handle('open-path', (event, path) => {
  shell.openPath(path)
})

app.whenReady().then(() => {
  startJavaBackend()
  createWindow()

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
