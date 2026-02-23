<template>
  <div class="book-grid">
    <el-empty v-if="books.length === 0" description="暂无书籍" />
    <div v-else class="grid">
      <div
        v-for="book in books"
        :key="book.id"
        class="book-card"
        @click="$emit('select', book)"
      >
        <div class="book-cover">
          <img :src="book.cover || '/default-cover.png'" :alt="book.title" />
          <div class="book-actions">
            <el-button
              size="small"
              circle
              @click.stop="toggleFavorite(book)"
            >
              <el-icon><Star :fill="book.isFavorite ? '#f59e0b' : 'none'" /></el-icon>
            </el-button>
            <el-button
              size="small"
              circle
              type="danger"
              @click.stop="$emit('delete', book)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="book-info">
          <div class="book-title" :title="book.title">{{ book.title }}</div>
          <div class="book-author">{{ book.author }}</div>
          <div class="book-progress">
            进度: {{ book.progressPercent || 0 }}%
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
 
<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Star, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
 
defineProps<{
  books: any[]
}>()
 
defineEmits(['select', 'delete'])
 
const toggleFavorite = async (book: any) => {
  try {
    await axios.put(`/api/books/${book.id}`, {
      isFavorite: !book.isFavorite
    })
    book.isFavorite = !book.isFavorite
    ElMessage.success(book.isFavorite ? '已收藏' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}
</script>
 
<style scoped>
.book-grid {
  min-height: 400px;
}
 
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
}
 
.book-card {
  cursor: pointer;
  transition: transform 0.2s;
}
 
.book-card:hover {
  transform: translateY(-5px);
}
 
.book-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
 
.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
 
.book-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.2s;
}
 
.book-card:hover .book-actions {
  opacity: 1;
}
 
.book-info {
  padding: 10px 5px;
}
 
.book-title {
  font-size: 14px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
 
.book-author {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}
 
.book-progress {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
}
</style>
