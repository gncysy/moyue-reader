<template>
  <div class="sources">
    <div class="header">
      <h1>📚 书源管理</h1>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书源..."
          prefix-icon="Search"
          style="width: 300px; margin-right: 10px"
          @input="handleSearch"
        />
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          新建书源
        </el-button>
        <el-button @click="showImportDialog">
          <el-icon><Upload /></el-icon>
          导入书源
        </el-button>
        <el-button @click="exportSources" :disabled="selectedSources.length === 0">
          <el-icon><Download /></el-icon>
          导出选中
        </el-button>
      </div>
    </div>
 
    <el-table
      :data="filteredSources"
      stripe
      @selection-change="handleSelectionChange"
      style="width: 100%"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="group" label="分组" width="100" />
      <el-table-column label="安全评级" width="150">
        <template #default="{ row }">
          <el-rate
            v-model="row.securityRating"
            disabled
            show-score
            text-color="#ff9900"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            @change="toggleSource(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="editSource(row)">
            编辑
          </el-button>
          <el-button size="small" @click="testSource(row)" :loading="testing === row.id">
            测试
          </el-button>
          <el-button size="small" @click="showSourceCode(row)">
            源码
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteSource(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
 
    <!-- 新建/编辑书源对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editingSource.id ? '编辑书源' : '新建书源'"
      width="800px"
    >
      <el-form :model="editingSource" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="editingSource.name" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="editingSource.author" />
        </el-form-item>
        <el-form-item label="URL">
          <el-input v-model="editingSource.url" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="editingSource.group" />
        </el-form-item>
        <el-form-item label="安全评级">
          <el-rate v-model="editingSource.securityRating" />
        </el-form-item>
        <el-form-item label="书源代码">
          <MonacoEditor
            v-model="editingSource.code"
            language="javascript"
            height="400px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSource">保存</el-button>
      </template>
    </el-dialog>
 
    <!-- 导入书源对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入书源" width="500px">
      <el-tabs v-model="importTab">
        <el-tab-pane label="从代码导入" name="code">
          <el-input
            v-model="importCode"
            type="textarea"
            :rows="10"
            placeholder="粘贴书源代码"
          />
        </el-tab-pane>
        <el-tab-pane label="从文件导入" name="file">
          <el-upload
            drag
            action="/api/sources/import"
            :on-success="handleImportSuccess"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
          </el-upload>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="importFromCode">导入</el-button>
      </template>
    </el-dialog>
 
    <!-- 源码查看对话框 -->
    <el-dialog
      v-model="codeDialogVisible"
      title="书源源码"
      width="900px"
    >
      <MonacoEditor
        v-model="viewingSourceCode"
        language="javascript"
        height="500px"
        :options="{ readOnly: true }"
      />
    </el-dialog>
  </div>
</template>
 
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Upload, Download, Search, UploadFilled
} from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { useSourceStore } from '@/stores/source'
import type { BookSource } from '@/stores/source'
 
const router = useRouter()
const sourceStore = useSourceStore()
 
const searchKeyword = ref('')
const selectedSources = ref<string[]>([])
const testing = ref('')
const editDialogVisible = ref(false)
const importDialogVisible = ref(false)
const importTab = ref('code')
const codeDialogVisible = ref(false)
const importCode = ref('')
const viewingSourceCode = ref('')
 
const editingSource = ref<Partial<BookSource>>({
  name: '',
  author: '',
  url: '',
  group: '',
  code: '',
  securityRating: 3,
  enabled: true
})
 
const sources = computed(() => sourceStore.sources)
 
const filteredSources = computed(() => {
  if (!searchKeyword.value) return sources.value
  const keyword = searchKeyword.value.toLowerCase()
  return sources.value.filter(source =>
    source.name.toLowerCase().includes(keyword) ||
    source.author?.toLowerCase().includes(keyword) ||
    source.url.toLowerCase().includes(keyword)
  )
})
 
const handleSearch = () => {
  // 搜索在 computed 中处理
}
 
const handleSelectionChange = (selection: BookSource[]) => {
  selectedSources.value = selection.map(s => s.id)
}
 
const showAddDialog = () => {
  editingSource.value = {
    name: '',
    author: '',
    url: '',
    group: '',
    code: '',
    securityRating: 3,
    enabled: true
  }
  editDialogVisible.value = true
}
 
const editSource = (source: BookSource) => {
  editingSource.value = { ...source }
  editDialogVisible.value = true
}
 
const saveSource = async () => {
  try {
    if (editingSource.value.id) {
      await sourceStore.updateSource(editingSource.value.id, editingSource.value)
      ElMessage.success('更新成功')
    } else {
      await sourceStore.createSource(editingSource.value)
      ElMessage.success('创建成功')
    }
    editDialogVisible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  }
}
 
const deleteSource = async (source: BookSource) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除书源 "${source.name}" 吗？`,
      '警告',
      { type: 'warning' }
    )
    await sourceStore.deleteSource(source.id)
    ElMessage.success('删除成功')
  } catch (error) {
    // 用户取消
  }
}
 
const testSource = async (source: BookSource) => {
  testing.value = source.id
  try {
    const result = await sourceStore.testSource(source.id)
    ElMessage.success(`测试成功: ${result.message}`)
  } catch (error) {
    ElMessage.error('测试失败')
  } finally {
    testing.value = ''
  }
}
 
const toggleSource = async (source: BookSource) => {
  try {
    await sourceStore.updateSource(source.id, { enabled: source.enabled })
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('更新失败')
  }
}
 
const showImportDialog = () => {
  importCode.value = ''
  importDialogVisible.value = true
}
 
const importFromCode = async () => {
  if (!importCode.value.trim()) {
    ElMessage.warning('请输入书源代码')
    return
  }
  
  try {
    await sourceStore.importSource(importCode.value)
    ElMessage.success('导入成功')
    importDialogVisible.value = false
  } catch (error) {
    ElMessage.error('导入失败')
  }
}
 
const handleImportSuccess = (response: any) => {
  ElMessage.success('导入成功')
  importDialogVisible.value = false
  sourceStore.fetchSources()
}
 
const exportSources = async () => {
  try {
    const data = await sourceStore.exportSources(selectedSources.value)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'sources.json'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}
 
const showSourceCode = async (source: BookSource) => {
  try {
    const detail = await sourceStore.fetchSource(source.id)
    viewingSourceCode.value = detail.code
    codeDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载源码失败')
  }
}
 
onMounted(async () => {
  await sourceStore.fetchSources()
})
</script>
 
<style scoped>
.sources {
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
 
.el-icon--upload {
  font-size: 67px;
  color: #409eff;
}
</style>
