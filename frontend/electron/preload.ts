import { contextBridge, ipcRenderer, IpcRendererEvent } from 'electron'
 
// 类型定义
interface WindowAPI {
  // 窗口控制
  window: {
    minimize: () => void
    maximize: () => Promise<{ isMaximized: boolean }>
    close: () => void
    onMaximizedChanged: (callback: (isMaximized: boolean) => void) => () => void
  }
  
  // 应用信息
  app: {
    getVersion: () => Promise<string>
    getPath: (name: string) => Promise<string>
    quit: () => void
  }
  
  // 后端控制
  backend: {
    getStatus: () => Promise<BackendStatus>
    restart: () => Promise<boolean>
    onReady: (callback: (status: { port: number }) => void) => () => void
    onStartupFailed: (callback: (error: { error: string }) => void) => () => void
  }
  
  // 主题
  theme: {
    getNativeTheme: () => Promise<{ shouldUseDarkColors: boolean }>
    onChanged: (callback: (theme: { shouldUseDarkColors: boolean }) => void) => () => void
  }
  
  // 自动更新
  updater: {
    checkForUpdates: () => Promise<void>
    onAvailable: (callback: (info: UpdateInfo) => void) => () => void
    onDownloadProgress: (callback: (progress: DownloadProgress) => void) => () => void
    onDownloaded: (callback: (info: UpdateInfo) => void) => () => void
  }
  
  // 系统
  system: {
    openExternal: (url: string) => Promise<void>
    openPath: (path: string) => Promise<void>
    showItemInFolder: (path: string) => Promise<void>
    selectFile: (options: FileOpenOptions) => Promise<string[] | null>
    selectDirectory: (options: DirectoryOpenOptions) => Promise<string | null>
  }
  
  // 日志
  log: {
    info: (message: string) => void
    error: (message: string) => void
  }
}
 
interface BackendStatus {
  running: boolean
  ready: boolean
  port?: number
}
 
interface UpdateInfo {
  version: string
  releaseName?: string
  releaseNotes?: string
  releaseDate?: string
}
 
interface DownloadProgress {
  percent: number
  transferredBytes: number
  totalBytes: number
  bytesPerSecond: number
}
 
interface FileOpenOptions {
  title?: string
  buttonLabel?: string
  filters?: Array<{ name: string; extensions: string[] }>
  properties?: Array<'openFile' | 'openDirectory' | 'multiSelections' | 'showHiddenFiles'>
}
 
interface DirectoryOpenOptions {
  title?: string
  buttonLabel?: string
  properties?: Array<'openDirectory' | 'createDirectory' | 'multiSelections' | 'showHiddenFiles'>
}
 
// 白名单验证
const allowedChannels = new Set([
  'window-maximized-changed',
  'backend-ready',
  'backend-startup-failed',
  'theme-changed',
  'update-available',
  'update-download-progress',
  'update-downloaded'
])
 
// 创建监听器包装器
function createListener(
  channel: string,
  callback: (...args: any[]) => void
): () => void {
  if (!allowedChannels.has(channel)) {
    console.warn(`[Preload] 不允许监听频道: ${channel}`)
    return () => {}
  }
  
  const wrappedCallback = (_event: IpcRendererEvent, ...args: any[]) => {
    callback(...args)
  }
  
  ipcRenderer.on(channel, wrappedCallback)
  
  // 返回移除监听器的函数
  return () => {
    ipcRenderer.removeListener(channel, wrappedCallback)
  }
}
 
// 暴露 API
const api: WindowAPI = {
  // 窗口控制
  window: {
    minimize: () => ipcRenderer.send('window-minimize'),
    maximize: () => ipcRenderer.invoke('window-maximize'),
    close: () => ipcRenderer.send('window-close'),
    onMaximizedChanged: (callback) => 
      createListener('window-maximized-changed', (isMaximized: boolean) => callback(isMaximized))
  },
  
  // 应用信息
  app: {
    getVersion: () => ipcRenderer.invoke('get-app-version'),
    getPath: (name: string) => ipcRenderer.invoke('get-app-path', name),
    quit: () => ipcRenderer.send('app-quit')
  },
  
  // 后端控制
  backend: {
    getStatus: () => ipcRenderer.invoke('get-backend-status'),
    restart: () => ipcRenderer.invoke('restart-backend'),
    onReady: (callback) => 
      createListener('backend-ready', (status: { port: number }) => callback(status)),
    onStartupFailed: (callback) => 
      createListener('backend-startup-failed', (error: { error: string }) => callback(error))
  },
  
  // 主题
  theme: {
    getNativeTheme: () => ipcRenderer.invoke('get-native-theme'),
    onChanged: (callback) => 
      createListener('theme-changed', (theme: { shouldUseDarkColors: boolean }) => callback(theme))
  },
  
  // 自动更新
  updater: {
    checkForUpdates: () => ipcRenderer.invoke('check-for-updates'),
    onAvailable: (callback) => 
      createListener('update-available', (info: UpdateInfo) => callback(info)),
    onDownloadProgress: (callback) => 
      createListener('update-download-progress', (progress: DownloadProgress) => callback(progress)),
    onDownloaded: (callback) => 
      createListener('update-downloaded', (info: UpdateInfo) => callback(info))
  },
  
  // 系统
  system: {
    openExternal: (url: string) => ipcRenderer.invoke('open-external', url),
    openPath: (path: string) => ipcRenderer.invoke('open-path', path),
    showItemInFolder: (path: string) => ipcRenderer.invoke('show-item-in-folder', path),
    selectFile: (options: FileOpenOptions) => ipcRenderer.invoke('select-file', options),
    selectDirectory: (options: DirectoryOpenOptions) => ipcRenderer.invoke('select-directory', options)
  },
  
  // 日志
  log: {
    info: (message: string) => {
      console.log(`[Renderer] ${message}`)
      ipcRenderer.send('log-info', message)
    },
    error: (message: string) => {
      console.error(`[Renderer Error] ${message}`)
      ipcRenderer.send('log-error', message)
    }
  }
}
 
// 暴露到渲染进程
contextBridge.exposeInMainWorld('electron', api)
 
// 类型声明供 TypeScript 使用
declare global {
  interface Window {
    electron: WindowAPI
  }
}
 
export {}
