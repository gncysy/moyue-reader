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

  // ==================== 禁用所有开发者功能 ====================

  if (!isDev) {
    mainWindow.webContents.on('devtools-opened', () => {
      mainWindow?.webContents.closeDevTools()
    })
  }

  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'F12' || 
        (input.control && input.shift && input.key === 'I') ||
        (input.meta && input.alt && input.key === 'I') ||
        input.key === 'F5' ||
        (input.control && input.key === 'r') ||
        (input.meta && input.key === 'r') ||
        (input.control && input.shift && input.key === 'J') ||
        (input.control && input.key === 'u')) {
      event.preventDefault()
    }
  })

  // ==================== 自定义右键菜单 ====================

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
        {
          label: `🔍 搜索 "${params.selectionText.substring(0, 20)}${params.selectionText.length > 20 ? '...' : ''}"`,
          click: () => {
            const text = encodeURIComponent(params.selectionText)
            shell.openExternal(`https://www.baidu.com/s?wd=${text}`)
          }
        },
        { type: 'separator' }
      )
    }
    
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
            shell.openExternal(params.srcURL)
          }
        },
        { type: 'separator' }
      )
    }
    
    if (params.linkURL && params.linkURL.trim().length > 0) {
      menuTemplate.push(
        {
          label: '🔗 复制链接地址',
          click: () => {
            shell.clipboard.writeText(params.linkURL)
          }
        },
        {
          label: '🔗 在新窗口打开链接',
          click: () => {
            shell.openExternal(params.linkURL)
          }
        },
        { type: 'separator' }
      )
    }
    
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

// ==================== 启动 Java 后端（无窗口版）====================

function startJavaBackend() {
  const isDev = process.env.NODE_ENV === 'development'
  
  let javaPath = 'java'
  let jarPath = ''
  
  if (!isDev) {
    // 生产环境：使用打包的 JRE，并用 javaw.exe 隐藏窗口
    const jrePath = path.join(process.resourcesPath, 'jre', 'bin', 'javaw.exe')
    if (fs.existsSync(jrePath)) {
      javaPath = jrePath
    } else {
      // 备用：用 java.exe 但隐藏窗口
      javaPath = path.join(process.resourcesPath, 'jre', 'bin', 'java.exe')
    }
    jarPath = path.join(process.resourcesPath, 'app.asar.unpacked', 'backend', 'moyue-backend.jar')
    
    console.log('启动后端服务（无窗口模式）')
    
    // 使用 detached + stdio ignore + windowsHide 彻底隐藏窗口
    javaProcess = spawn(javaPath, ['-Xshare:auto', '-jar', jarPath, '--server.port=0'], {
      detached: true,
      stdio: 'ignore',
      windowsHide: true
    })
    
    // 允许父进程独立退出
    javaProcess.unref()
    
    // 等待后端启动（简单轮询）
    let retries = 0
    const checkBackend = setInterval(() => {
      http.get('http://localhost:8080/api/health', (res) => {
        if (res.statusCode === 200) {
          clearInterval(checkBackend)
          console.log('✅ 后端启动成功')
          if (mainWindow) {
            mainWindow.webContents.send('backend-ready')
          }
        }
      }).on('error', () => {
        retries++
        if (retries > 30) {
          clearInterval(checkBackend)
          console.error('❌ 后端启动超时')
        }
      })
    }, 1000)
    
  } else {
    // 开发环境：正常显示，用于调试
    jarPath = path.join(__dirname, '../../backend/build/libs/moyue-backend.jar')
    console.log('启动后端（开发模式）:', jarPath)
    
    javaProcess = spawn(javaPath, ['-jar', jarPath, '--server.port=0'], {
      stdio: 'pipe'
    })
    
    javaProcess.stdout?.on('data', (data: Buffer) => {
      console.log(`[Java] ${data.toString().trim()}`)
      
      // 从日志中捕获实际端口
      const output = data.toString()
      const match = output.match(/Tomcat started on port\(s\): (\d+)/)
      if (match && mainWindow) {
        const port = match[1]
        console.log(`✅ 后端实际端口: ${port}`)
        mainWindow.webContents.send('backend-port', port)
      }
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
  shell.openExternal(url)
})

ipcMain.handle('open-path', (event, path) => {
  shell.openPath(path)
})

ipcMain.handle('check-backend', async () => {
  return new Promise((resolve) => {
    http.get('http://localhost:8080/api/health', (res) => {
      resolve(res.statusCode === 200)
    }).on('error', () => {
      resolve(false)
    })
  })
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
