import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
 
// 路由定义
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: '书架',
      icon: 'Books',
      darkMode: false
    }
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/Home.vue'), // 临时复用 Home，实际应该有 Discover.vue
    meta: {
      title: '发现',
      icon: 'Search',
      darkMode: false
    }
  },
  {
    path: '/sources',
    name: 'Sources',
    component: () => import('@/views/Sources.vue'),
    meta: {
      title: '书源',
      icon: 'Document',
      darkMode: false
    }
  },
  {
    path: '/security',
    name: 'Security',
    component: () => import('@/views/Security.vue'),
    meta: {
      title: '安全',
      icon: 'Lock',
      darkMode: false
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Security.vue'), // 临时复用 Security，实际应该有 Settings.vue
    meta: {
      title: '设置',
      icon: 'Setting',
      darkMode: false
    }
  },
  {
    path: '/debug',
    name: 'SourceDebugger',
    component: () => import('@/views/SourceDebugger.vue'),
    meta: {
      title: '书源调试',
      icon: 'Tools',
      darkMode: false,
      hidden: true // 隐藏的调试页面
    }
  }
]
 
// 创建路由
const router = createRouter({
  history: createWebHashHistory(),
  routes
})
 
// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 墨阅`
  }
  
  // 检查后端状态（如果需要）
  // if (to.meta.requiresBackend && !backendReady.value) {
  //   next('/')
  //   return
  // }
  
  next()
})
 
export default router
