import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/sources',
    name: 'Sources',
    component: () => import('@/views/Sources.vue')
  },
  {
    path: '/security',
    name: 'Security',
    component: () => import('@/views/Security.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
