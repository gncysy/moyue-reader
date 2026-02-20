import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electron', {
  // 窗口控制
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.invoke('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
  
  // 系统功能
  getAppPath: () => ipcRenderer.invoke('get-app-path'),
  openExternal: (url: string) => ipcRenderer.invoke('open-external', url),
  openPath: (path: string) => ipcRenderer.invoke('open-path', path),
  
  // 事件监听
  on: (channel: string, callback: (data: any) => void) => {
    ipcRenderer.on(channel, (event, data) => callback(data))
  }
})
