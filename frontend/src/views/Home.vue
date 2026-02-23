<template>
  <div class="home">
    <div class="header">
      <h1>📚 我的书架</h1>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书籍..."
          prefix-icon="Search"
          style="width: 300px; margin-right: 10px"
          @input="handleSearch"
        />
        <el-button type="primary" @click="showAddBookDialog">
          <el-icon><Plus /></el-icon>
          添加书籍
        </el-button>
        <el-button @click="refreshBooks">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
 
    <el-tabs v-model="activeTab" class="book-tabs">
      <el-tab-pane label="全部" name="all">
        <BookGrid :books="filteredBooks" @select="handleBookSelect" @delete="handleBookDelete" />
      </el-tab-pane>
      <el-tab-pane label="最近阅读" name="recent">
        <BookGrid :books="recentBooks" @select="handleBookSelect" @delete="handleBookDelete" />
      </el-tab-pane>
      <el-tab-pane label="收藏" name="favorite">
        <BookGrid :books="favoriteBooks" @select="handleBookSelect" @delete="handleBookDelete" />
      </el-tab-pane>
    </el-tabs>
 
    <!-- 添加书籍对话框 -->
    <el-dialog v-model="addBookDialogVisible" title="添加书籍" width="500px">
      <el-form :model="newBook" label-width="80px">
        <el-form-item label="书源">
          <el-select v-model="newBook.sourceId" placeholder="选择书源">
            <el-option
              v-for="source in sources"
              :key="source.id"
              :label="source.name"
              :value="source.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索词">
          <el-input v-model="newBook.searchKeyword" placeholder="输入书名或作者" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addBookDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddBook" :loading="searching">搜索并添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>
 
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import { useSourceStore } from '@/stores/source'
import BookGrid from '@/components/BookGrid.vue'
 
const router = useRouter()
const sourceStore = useSourceStore()
 
const books = ref<any[]>([])
const sources = ref<any[]>([])
const searchKeyword = ref('')
const activeTab = ref('all')
const addBookDialogVisible = ref(false)
const searching = ref(false)
 
const newBook = ref({
  sourceId: '',
  searchKeyword: ''
})
 
const filteredBooks = computed(() => {
  if (!searchKeyword.value) return books.value
  const keyword = searchKeyword.value.toLowerCase()
  return books.value.filter(book => 
    book.title.toLowerCase().includes(keyword) ||
    book.author.toLowerCase().includes(keyword)
  )
})
 
const recentBooks = computed(() => {
  return [...books.value]
    .sort((a, b) => new Date(b.lastReadTime).getTime() - new Date(a.lastReadTime).getTime())
    .slice(0, 20)
})
 
const favoriteBooks = computed(() => {
  return books.value.filter(book => book.isFavorite)
})
 
const handleSearch = () => {
  // 搜索逻辑在 computed 中处理
}
 
const handleBookSelect = (book: any) => {
  router.push(`/read/${book.id}`)
}
 
const handleBookDelete = async (book: any) => {
  try {
    await axios.delete(`/api/books/${book.id}`)
    books.value = books.value.filter(b => b.id !== book.id)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}
 
const showAddBookDialog = () => {
  addBookDialogVisible.value = true
}
 
const handleAddBook = async () => {
  if (!newBook.value.sourceId || !newBook.value.searchKeyword) {
    ElMessage.warning('请填写完整信息')
    return
  }
 
  searching.value = true
  try {
    const res = await axios.post('/api/books/search', {
      sourceId: newBook.value.sourceId,
      keyword: newBook.value.searchKeyword
    })
    
    if (res.data.length === 0) {
      ElMessage.warning('未找到匹配的书籍')
      return
    }
 
    // 如果只有一个结果，直接添加
    if (res.data.length === 1) {
      await addBookToShelf(res.data[0])
      return
    }
 
    // 显示搜索结果让用户选择
    ElMessage.success('找到 ' + res.data.length + ' 本书，请手动选择添加')
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}
 
const addBookToShelf = async (bookInfo: any) => {
  try {
    const res = await axios.post('/api/books', bookInfo)
    books.value.push(res.data)
    addBookDialogVisible.value = false
    ElMessage.success('添加成功')
  } catch (error) {
    ElMessage.error('添加失败')
  }
}
 
const refreshBooks = async () => {
  try {
    const res = await axios.get('/api/books')
    books.value = res.data
    ElMessage.success('刷新成功')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}
 
onMounted(async () => {
  await sourceStore.fetchSources()
  sources.value = sourceStore.sources
  await refreshBooks()
})
</script>
 
<style scoped>
.home {
  padding: 20px;
}
 
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
 
.header h1 {
  margin: 0;
  font-size: 24px;
}
 
.header-actions {
  display: flex;
  align-items: center;
}
 
.book-tabs {
  margin-top: 20px;
}
</style>
