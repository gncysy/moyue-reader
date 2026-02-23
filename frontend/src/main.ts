import { createApp, type App as VueApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
 
// 类型定义
declare global {
  interface Window {
    electron?: any
  }
}
 
// 全局错误处理器
const errorHandler = (error: any, vm: any, info: string) => {
  console.error('[Vue Error]', error)
  console.error('[Error Info]', info)
  
  // 开发环境更详细的错误信息
  if (import.meta.env.DEV) {
    console.error('[Component]', vm)
  }
  
  // 生产环境可以上报错误
  if (import.meta.env.PROD && window.electron) {
    window.electron.log.error(error.message || 'Unknown error')
  }
}
 
// 警告处理器
const warnHandler = (msg: string, vm: any, trace: string) => {
  if (import.meta.env.DEV) {
    console.warn('[Vue Warning]', msg)
    console.warn('[Trace]', trace)
  }
}
 
// 创建 Vue 应用
const app: VueApp = createApp(App)
 
// 配置 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)
 
// 配置路由
app.use(router)
 
// 配置 Element Plus
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default',
  zIndex: 3000,
})
 
// 全局错误处理
app.config.errorHandler = errorHandler
app.config.warnHandler = warnHandler
 
// 开发环境配置
if (import.meta.env.DEV) {
  app.config.devtools = true
  
  // 开发模式打印环境信息
  console.log('[Moyue] 开发模式')
  console.log('[Moyue] Electron API:', typeof window.electron !== 'undefined')
  
  // 挂载到 window 方便调试
  window.__VUE_DEVTOOLS_GLOBAL_HOOK__ = app
}
 
// 生产环境配置
if (import.meta.env.PROD) {
  // 生产环境优化
  app.config.performance = false
  
  // 禁用 Vue 警告
  app.config.warnHandler = () => {}
}
 
// Electron 相关初始化
if (typeof window.electron !== 'undefined') {
  console.log('[Moyue] 运行在 Electron 环境')
  
  // 监听后端就绪事件
  window.electron.backend.onReady((status: { port: number }) => {
    console.log('[Moyue] 后端就绪，端口:', status.port)
  })
  
  // 监听后端启动失败事件
  window.electron.backend.onStartupFailed((error: { error: string }) => {
    console.error('[Moyue] 后端启动失败:', error.error)
  })
  
  // 监听主题变化
  window.electron.theme.onChanged((theme: { shouldUseDarkColors: boolean }) => {
    console.log('[Moyue] 主题变化:', theme.shouldUseDarkColors)
    document.documentElement.classList.toggle('dark', theme.shouldUseDarkColors)
  })
  
  // 初始化主题
  window.electron.theme.getNativeTheme().then((theme: { shouldUseDarkColors: boolean }) => {
    document.documentElement.classList.toggle('dark', theme.shouldUseDarkColors)
  })
} else {
  console.log('[Moyue] 运行在浏览器环境（开发模式）')
}
 
// 全局样式
import './styles/index.scss'
 
// 挂载应用
app.mount('#app')
 
// 导出 app 实例（用于测试或插件）
export default app
