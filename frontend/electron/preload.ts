import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electron', {
  getAppPath: () => ipcRenderer.invoke('get-app-path'),
  openExternal: (url: string) => ipcRenderer.invoke('open-external', url),
  on: (channel: string, callback: (data: any) => void) => {
    ipcRenderer.on(channel, (event, data) => callback(data))
  },
  send: (channel: string, data: any) => {
    ipcRenderer.send(channel, data)
  }
})
