import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
 
export interface BookSource {
  id: string
  name: string
  url: string
  group: string
  enabled: boolean
  securityRating: number
  code: string
  author?: string
  version?: string
}
 
export const useSourceStore = defineStore('source', () => {
  const sources = ref<BookSource[]>([])
  const loading = ref(false)
  const currentSource = ref<BookSource | null>(null)
 
  // 获取所有书源
  const fetchSources = async () => {
    loading.value = true
    try {
      const res = await axios.get('/api/sources')
      sources.value = res.data
    } catch (error) {
      console.error('获取书源失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
 
  // 获取单个书源详情
  const fetchSource = async (id: string) => {
    try {
      const res = await axios.get(`/api/sources/${id}`)
      currentSource.value = res.data
      return res.data
    } catch (error) {
      console.error('获取书源详情失败:', error)
      throw error
    }
  }
 
  // 创建书源
  const createSource = async (source: Partial<BookSource>) => {
    try {
      const res = await axios.post('/api/sources', source)
      sources.value.push(res.data)
      return res.data
    } catch (error) {
      console.error('创建书源失败:', error)
      throw error
    }
  }
 
  // 更新书源
  const updateSource = async (id: string, source: Partial<BookSource>) => {
    try {
      const res = await axios.put(`/api/sources/${id}`, source)
      const index = sources.value.findIndex(s => s.id === id)
      if (index !== -1) {
        sources.value[index] = res.data
      }
      return res.data
    } catch (error) {
      console.error('更新书源失败:', error)
      throw error
    }
  }
 
  // 删除书源
  const deleteSource = async (id: string) => {
    try {
      await axios.delete(`/api/sources/${id}`)
      sources.value = sources.value.filter(s => s.id !== id)
    } catch (error) {
      console.error('删除书源失败:', error)
      throw error
    }
  }
 
  // 导入书源
  const importSource = async (code: string) => {
    try {
      const res = await axios.post('/api/sources/import', { code })
      sources.value.push(res.data)
      return res.data
    } catch (error) {
      console.error('导入书源失败:', error)
      throw error
    }
  }
 
  // 导出书源
  const exportSources = async (ids: string[]) => {
    try {
      const res = await axios.post('/api/sources/export', { ids })
      return res.data
    } catch (error) {
      console.error('导出书源失败:', error)
      throw error
    }
  }
 
  // 测试书源
  const testSource = async (id: string) => {
    try {
      const res = await axios.post(`/api/sources/${id}/test`)
      return res.data
    } catch (error) {
      console.error('测试书源失败:', error)
      throw error
    }
  }
 
  return {
    sources,
    loading,
    currentSource,
    fetchSources,
    fetchSource,
    createSource,
    updateSource,
    deleteSource,
    importSource,
    exportSources,
    testSource
  }
})
