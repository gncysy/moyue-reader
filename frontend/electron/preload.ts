import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electron', {
  // 窗口控制
  send: (channel: string, ...args: any[]) => {
    ipcRenderer.send(channel, ...args)
  },
  invoke: (channel: string, ...args: any[]) => {
    return ipcRenderer.invoke(channel, ...args)
  },
  on: (channel: string, callback: (...args: any[]) => void) => {
    ipcRenderer.on(channel, (event, ...args) => callback(...args))
  },
  
  // 原有功能
  getAppPath: () => ipcRenderer.invoke('get-app-path'),
  openExternal: (url: string) => ipcRenderer.invoke('open-external', url),
  openPath: (path: string) => ipcRenderer.invoke('open-path', path)
})
