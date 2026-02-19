<template>
  <el-config-provider :locale="zhCn">
    <div class="app" :class="{ 'dark': isDarkMode }">
      <!-- 自定义标题栏 -->
      <TitleBar 
        title="墨阅" 
        :is-dark="isDarkMode"
      >
        <!-- 可以在标题栏中间放导航 -->
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
      
      <!-- 主内容区 -->
      <div class="main-content">
        <router-view />
      </div>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import TitleBar from '@/components/TitleBar.vue'

const route = useRoute()
const router = useRouter()

const currentRoute = computed(() => route.path)
const isDarkMode = computed(() => route.meta.darkMode || false)

const tabs = [
  { path: '/', name: '书架' },
  { path: '/discover', name: '发现' },
  { path: '/sources', name: '书源' },
  { path: '/settings', name: '设置' }
]

const navigateTo = (path: string) => {
  router.push(path)
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  overflow: hidden;
}

.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.app.dark {
  background-color: #1a1e24;
}

.main-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
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
</style>
