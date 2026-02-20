import { app, BrowserWindow, ipcMain, globalShortcut, Menu } from 'electron'
import path from 'path'
import { spawn } from 'child_process'
import fs from 'fs'

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
      // 禁用开发者工具
      devTools: isDev
    },
    icon: path.join(__dirname, '../build/icon.ico')
  })

  // 移除系统菜单栏（彻底干掉）
  mainWindow.removeMenu()

  // ==================== 禁用所有开发者功能 ====================

  // 1. 监听并强制关闭任何打开的开发者工具
  if (!isDev) {
    mainWindow.webContents.on('devtools-opened', () => {
      mainWindow?.webContents.closeDevTools()
    })
  }

  // 2. 阻止所有快捷键（包括F12、Ctrl+Shift+I、F5等）
  mainWindow.webContents.on('before-input-event', (event, input) => {
    // F12
    if (input.key === 'F12') {
      event.preventDefault()
    }
    // Ctrl+Shift+I (Windows/Linux)
    if (input.control && input.shift && input.key === 'I') {
      event.preventDefault()
    }
    // Cmd+Option+I (Mac)
    if (input.meta && input.alt && input.key === 'I') {
      event.preventDefault()
    }
    // F5 刷新
    if (input.key === 'F5') {
      event.preventDefault()
    }
    // Ctrl+R 刷新
    if (input.control && input.key === 'r') {
      event.preventDefault()
    }
    // Cmd+R 刷新 (Mac)
    if (input.meta && input.key === 'r') {
      event.preventDefault()
    }
    // Ctrl+Shift+J (打开控制台)
    if (input.control && input.shift && input.key === 'J') {
      event.preventDefault()
    }
    // Ctrl+U (查看源代码)
    if (input.control && input.key === 'u') {
      event.preventDefault()
    }
  })

  // ==================== 自定义右键菜单 ====================

  mainWindow.webContents.on('context-menu', (event, params) => {
    event.preventDefault()
    
    const menuTemplate: any[] = []
    
    // 1. 如果有选中文字，显示复制相关选项
    if (params.selectionText && params.selectionText.trim().length > 0) {
      menuTemplate.push(
        {
          label: '📋 复制',
          accelerator: 'Ctrl+C',
          click: () => {
            mainWindow?.webContents.copy()
          }
        },
        {
          label: `🔍 搜索 "${params.selectionText.substring(0, 20)}${params.selectionText.length > 20 ? '...' : ''}"`,
          click: () => {
            const text = encodeURIComponent(params.selectionText)
            mainWindow?.webContents.loadURL(`https://www.baidu.com/s?wd=${text}`)
          }
        },
        { type: 'separator' }
      )
    }
    
    // 2. 如果点击的是图片，显示图片相关选项
    if (params.mediaType === 'image') {
      menuTemplate.push(
        {
          label: '🖼️ 复制图片地址',
          click: () => {
            mainWindow?.webContents.copyImageAt(params.x, params.y)
          }
        },
        {
          label: '🖼️ 在新窗口打开图片',
          click: () => {
            require('electron').shell.openExternal(params.srcURL)
          }
        },
        { type: 'separator' }
      )
    }
    
    // 3. 如果点击的是链接，显示链接相关选项
    if (params.linkURL && params.linkURL.trim().length > 0) {
      menuTemplate.push(
        {
          label: '🔗 复制链接地址',
          click: () => {
            mainWindow?.webContents.copy()
          }
        },
        {
          label: '🔗 在新窗口打开链接',
          click: () => {
            require('electron').shell.openExternal(params.linkURL)
          }
        },
        { type: 'separator' }
      )
    }
    
    // 4. 常用功能
    menuTemplate.push(
      {
        label: '🔄 刷新',
        accelerator: 'F5',
        click: () => {
          mainWindow?.webContents.reload()
        }
      },
      {
        label: '⬆️ 回到顶部',
        click: () => {
          mainWindow?.webContents.executeJavaScript('window.scrollTo(0,0)')
        }
      },
      {
        label: '⬇️ 回到底部',
        click: () => {
          mainWindow?.webContents.executeJavaScript('window.scrollTo(0, document.body.scrollHeight)')
        }
      }
    )
    
    // 如果菜单不为空，显示它
    if (menuTemplate.length > 0) {
      const menu = Menu.buildFromTemplate(menuTemplate)
      menu.popup({
        window: mainWindow!,
        x: params.x,
        y: params.y
      })
    }
  })

  // 加载界面
  if (isDev) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL || 'http://localhost:5173')
    // 开发环境自动打开开发者工具
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

// ==================== 启动 Java 后端 ====================

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

  // 使用随机端口（0 让 Spring Boot 随机选择）
  javaProcess = spawn(javaPath, ['-jar', jarPath, '--server.port=0'], {
    stdio: 'pipe',
    detached: false
  })

  // 从日志中捕获实际端口
  javaProcess.stdout?.on('data', (data: Buffer) => {
    const output = data.toString()
    console.log(`[Java] ${output.trim()}`)
    
    // 匹配 Spring Boot 实际端口
    const match = output.match(/Tomcat started on port\(s\): (\d+)/)
    if (match && mainWindow) {
      const port = match[1]
      console.log(`✅ 后端实际端口: ${port}`)
      
      // 可以在这里把端口传给渲染进程（如果需要）
      mainWindow.webContents.send('backend-port', port)
    }
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

// ==================== IPC 处理 ====================

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

// ==================== 监听窗口状态变化 ====================

function setupWindowListeners() {
  if (!mainWindow) return
  
  mainWindow.on('maximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', true)
  })

  mainWindow.on('unmaximize', () => {
    mainWindow?.webContents.send('window-maximized-changed', false)
  })
}

// ==================== 应用生命周期 ====================

app.whenReady().then(() => {
  // 全局快捷键拦截（即使窗口没焦点也能拦截）
  if (app.isPackaged) {
    globalShortcut.register('F12', () => {
      console.log('F12被拦截')
    })
    globalShortcut.register('CommandOrControl+Shift+I', () => {
      console.log('开发者工具快捷键被拦截')
    })
    globalShortcut.register('F5', () => {
      console.log('F5被拦截')
    })
    globalShortcut.register('CommandOrControl+R', () => {
      console.log('刷新快捷键被拦截')
    })
    globalShortcut.register('CommandOrControl+Shift+J', () => {
      console.log('控制台快捷键被拦截')
    })
  }
  
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

// 取消所有全局快捷键
app.on('will-quit', () => {
  globalShortcut.unregisterAll()
})
