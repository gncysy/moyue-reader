<template>
  <div class="title-bar" :class="{ 'dark': isDark }">
    <div class="title-bar-drag-area"></div>
    
    <div class="title-bar-left">
      <slot name="left">
        <div class="logo-area">
          <img v-if="logo" :src="logo" class="logo" />
          <span class="title">{{ title }}</span>
        </div>
      </slot>
    </div>
    
    <div class="title-bar-center">
      <slot name="center"></slot>
    </div>
    
    <div class="title-bar-right">
      <slot name="right">
        <div class="window-controls">
          <button class="window-control minimize" @click="minimize" title="最小化">
            <svg width="12" height="12" viewBox="0 0 12 12">
              <rect x="2" y="5" width="8" height="2" fill="currentColor" />
            </svg>
          </button>
          
          <button class="window-control maximize" @click="maximize" title="最大化">
            <svg v-if="!isMaximized" width="12" height="12" viewBox="0 0 12 12">
              <rect x="2" y="2" width="8" height="8" stroke="currentColor" fill="none" stroke-width="1.2" />
            </svg>
            <svg v-else width="12" height="12" viewBox="0 0 12 12">
              <rect x="2" y="2" width="8" height="8" stroke="currentColor" fill="none" stroke-width="1.2" />
              <line x1="4" y1="2" x2="4" y2="4" stroke="currentColor" stroke-width="1.2" />
              <line x1="2" y1="4" x2="4" y2="4" stroke="currentColor" stroke-width="1.2" />
            </svg>
          </button>
          
          <button class="window-control close" @click="close" title="关闭">
            <svg width="12" height="12" viewBox="0 0 12 12">
              <line x1="3" y1="3" x2="9" y2="9" stroke="currentColor" stroke-width="1.5" />
              <line x1="9" y1="3" x2="3" y2="9" stroke="currentColor" stroke-width="1.5" />
            </svg>
          </button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: '墨阅'
  },
  logo: {
    type: String,
    default: ''
  },
  isDark: {
    type: Boolean,
    default: false
  }
})

const isMaximized = ref(false)

// 窗口控制函数
const minimize = () => {
  window.electron?.send('window-minimize')
}

const maximize = async () => {
  const result = await window.electron?.invoke('window-maximize')
  isMaximized.value = result?.isMaximized || false
}

const close = () => {
  window.electron?.send('window-close')
}

// 监听最大化状态变化
onMounted(() => {
  window.electron?.on('window-maximized-changed', (_, maximized) => {
    isMaximized.value = maximized
  })
})
</script>

<style scoped>
.title-bar {
  height: 48px;
  background-color: #fff;
  border-bottom: 1px solid #eaeef2;
  display: flex;
  align-items: center;
  padding: 0 12px;
  position: relative;
  user-select: none;
  color: #1e2a3a;
}

.title-bar.dark {
  background-color: #1e2a3a;
  border-bottom-color: #2c3e50;
  color: #fff;
}

.title-bar-drag-area {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  -webkit-app-region: drag;
}

.title-bar-left,
.title-bar-center,
.title-bar-right {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 100%;
}

.title-bar-left {
  flex: 0 0 auto;
}

.title-bar-center {
  flex: 1;
  justify-content: center;
}

.title-bar-right {
  flex: 0 0 auto;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
  -webkit-app-region: drag;
}

.logo {
  height: 24px;
  width: auto;
}

.title {
  font-size: 14px;
  font-weight: 500;
  color: currentColor;
}

.window-controls {
  display: flex;
  gap: 8px;
  -webkit-app-region: no-drag;
}

.window-control {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: inherit;
  transition: all 0.2s;
}

.window-control:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.dark .window-control:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.window-control.close:hover {
  background-color: #e81123;
  color: white;
}

.window-control svg {
  display: block;
}
</style>
