<template>
  <div class="app" :class="{ 'dark': isDarkMode }">
    <!-- 自定义标题栏 -->
    <TitleBar 
      title="墨阅" 
      :is-dark="isDarkMode"
    >
      <template #center>
        <div class="nav-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.path"
            class="nav-tab"
            :class="{ active: currentRoute === tab.path }"
            @click="navigateTo(tab.path)"
          >
            {{ tab.name }}
          </button>
        </div>
      </template>
    </TitleBar>
    
    <!-- 后端状态提示 -->
    <Transition name="fade">
      <div v-if="backendStatus.show" class="backend-status" :class="backendStatus.type">
        <el-icon>
          <Loading v-if="backendStatus.type === 'loading'" />
          <Warning v-else-if="backendStatus.type === 'error'" />
          <SuccessFilled v-else />
        </el-icon>
        <span>{{ backendStatus.message }}</span>
        <el-button 
          v-if="backendStatus.type === 'error'" 
          text 
          size="small" 
          @click="retryBackend"
        >
          重试
        </el-button>
      </div>
    </Transition>
    
    <!-- 自动更新提示 -->
    <Transition name="slide-up">
      <div v-if="updateInfo" class="update-banner">
        <span>发现新版本 v{{ updateInfo.version }}</span>
        <el-button size="small" type="primary" @click="downloadUpdate">
          立即更新
        </el-button>
        <el-button size="small" @click="dismissUpdate">
          忽略
        </el-button>
      </div>
    </Transition>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <router-view v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" :key="currentRoute" />
        </Transition>
      </router-view>
    </div>
  </div>
</template>
 
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TitleBar from '@/components/TitleBar.vue'
import { Loading, Warning, SuccessFilled } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
 
const route = useRoute()
const router = useRouter()
 
// 暗黑模式
const isDarkMode = ref(false)
 
// 后端状态
const backendStatus = ref({
  show: false,
  type: 'loading' as 'loading' | 'success' | 'error',
  message: ''
})
 
// 更新信息
const updateInfo = ref<{ version: string } | null>(null)
 
// 当前路由
const currentRoute = computed(() => route.path)
 
// 导航标签
const tabs = [
  { path: '/', name: '书架' },
  { path: '/discover', name: '发现' },
  { path: '/sources', name: '书源' },
  { path: '/security', name: '安全' },
  { path: '/settings', name: '设置' }
]
 
// 导航
const navigateTo = (path: string) => {
  router.push(path)
}
 
// 检查后端状态
const checkBackendStatus = async () => {
  if (!window.electron) {
    // 开发模式模拟后端就绪
    setTimeout(() => {
      backendStatus.value = { show: false, type: 'success', message: '' }
    }, 1000)
    return
  }
  
  const status = await window.electron.backend.getStatus()
  
  if (status.running && status.ready) {
    backendStatus.value = { show: false, type: 'success', message: '' }
  } else {
    backendStatus.value = {
      show: true,
      type: 'loading',
      message: '后端启动中...'
    }
  }
}
 
// 重试启动后端
const retryBackend = async () => {
  if (!window.electron) return
  
  backendStatus.value = {
    show: true,
    type: 'loading',
    message: '正在重启后端...'
  }
  
  const success = await window.electron.backend.restart()
  
  if (success) {
    backendStatus.value = {
      show: true,
      type: 'success',
      message: '后端启动成功'
    }
    setTimeout(() => {
      backendStatus.value.show = false
    }, 2000)
  } else {
    backendStatus.value = {
      show: true,
      type: 'error',
      message: '后端启动失败，请查看日志'
    }
  }
}
 
// 下载更新
const downloadUpdate = () => {
  if (!window.electron) return
  window.electron.updater.checkForUpdates()
  updateInfo.value = null
}
 
// 忽略更新
const dismissUpdate = () => {
  updateInfo.value = null
}
 
// 后端就绪监听器
let unlistenBackendReady: (() => void) | null = null
let unlistenBackendFailed: (() => void) | null = null
let unlistenThemeChanged: (() => void) | null = null
let unlistenUpdateAvailable: (() => void) | null = null
 
onMounted(async () => {
  // 检查后端状态
  await checkBackendStatus()
  
  // 初始化主题
  if (window.electron) {
    const theme = await window.electron.theme.getNativeTheme()
    isDarkMode.value = theme.shouldUseDarkColors
    
    // 监听后端就绪
    unlistenBackendReady = window.electron.backend.onReady((status) => {
      backendStatus.value = {
        show: true,
        type: 'success',
        message: '后端启动成功'
      }
      setTimeout(() => {
        backendStatus.value.show = false
      }, 2000)
    })
    
    // 监听后端启动失败
    unlistenBackendFailed = window.electron.backend.onStartupFailed((error) => {
      backendStatus.value = {
        show: true,
        type: 'error',
        message: error.error || '后端启动失败'
      }
    })
    
    // 监听主题变化
    unlistenThemeChanged = window.electron.theme.onChanged((theme) => {
      isDarkMode.value = theme.shouldUseDarkColors
    })
    
    // 监听更新可用
    unlistenUpdateAvailable = window.electron.updater.onAvailable((info) => {
      updateInfo.value = info
      ElNotification({
        title: '发现新版本',
        message: `v${info.version} 已发布`,
        type: 'info',
        duration: 0
      })
    })
  }
})
 
onUnmounted(() => {
  // 清理监听器
  unlistenBackendReady?.()
  unlistenBackendFailed?.()
  unlistenThemeChanged?.()
  unlistenUpdateAvailable?.()
})
</script>
 
<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
 
:root {
  --app-bg-light: #f5f7fa;
  --app-bg-dark: #1a1e24;
  --text-light: #1e2a3a;
  --text-dark: #fff;
  --border-light: rgba(0, 0, 0, 0.06);
  --border-dark: rgba(255, 255, 255, 0.06);
}
 
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}
 
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--app-bg-light);
  color: var(--text-light);
  transition: background-color 0.3s, color 0.3s;
}
 
.app.dark {
  background-color: var(--app-bg-dark);
  color: var(--text-dark);
}
 
.main-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}
 
.nav-tabs {
  display: flex;
  gap: 4px;
  background-color: rgba(0, 0, 0, 0.04);
  padding: 4px;
  border-radius: 8px;
}
 
.dark .nav-tabs {
  background-color: rgba(255, 255, 255, 0.04);
}
 
.nav-tab {
  padding: 6px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #5f6b7a;
  cursor: pointer;
  transition: all 0.2s;
}
 
.dark .nav-tab {
  color: #a0aec0;
}
 
.nav-tab:hover {
  color: #1e2a3a;
  background-color: rgba(0, 0, 0, 0.04);
}
 
.dark .nav-tab:hover {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.04);
}
 
.nav-tab.active {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
}
 
.dark .nav-tab.active {
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.15);
}
 
/* 后端状态提示 */
.backend-status {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
 
.backend-status.loading {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}
 
.backend-status.success {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}
 
.backend-status.error {
  background-color: #fff2f0;
  color: #f5222d;
  border: 1px solid #ffccc7;
}
 
.dark .backend-status.loading {
  background-color: rgba(24, 144, 255, 0.15);
  color: #409EFF;
  border: 1px solid rgba(64, 158, 255, 0.3);
}
 
.dark .backend-status.success {
  background-color: rgba(82, 196, 26, 0.15);
  color: #67c23a;
  border: 1px solid rgba(103, 194, 58, 0.3);
}
 
.dark .backend-status.error {
  background-color: rgba(245, 34, 45, 0.15);
  color: #f56c6c;
  border: 1px solid rgba(245, 108, 108, 0.3);
}
 
/* 更新横幅 */
.update-banner {
  position: absolute;
  top: 60px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  z-index: 99;
}
 
/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
 
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
 
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
 
.slide-up-enter-from {
  transform: translateY(-20px);
  opacity: 0;
}
 
.slide-up-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}
</style>
