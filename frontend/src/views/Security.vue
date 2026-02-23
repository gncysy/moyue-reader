<template>
  <div class="security">
    <div class="header">
      <h1>🔒 安全中心</h1>
      <el-button type="primary" @click="saveSettings" :loading="saving">
        保存设置
      </el-button>
    </div>
 
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="security-card">
          <template #header>
            <span>🛡️ 安全级别</span>
          </template>
          
          <el-form label-width="100px">
            <el-form-item label="默认级别">
              <el-select v-model="securitySettings.defaultLevel">
                <el-option label="标准模式" value="standard" />
                <el-option label="兼容模式" value="compatible" />
                <el-option label="信任模式" value="trusted" />
              </el-select>
            </el-form-item>
            
            <div class="level-description">
              <h4>级别说明：</h4>
              <ul>
                <li><strong>标准模式：</strong> 最安全的模式，禁止所有危险操作</li>
                <li><strong>兼容模式：</strong> 允许部分 Android API，中等安全性</li>
                <li><strong>信任模式：</strong> 完全开放，仅用于可信书源</li>
              </ul>
            </div>
          </el-form>
        </el-card>
      </el-col>
 
      <el-col :span="12">
        <el-card class="security-card">
          <template #header>
            <span>🚫 禁止的操作</span>
          </template>
          
          <el-checkbox-group v-model="securitySettings.blockedOperations">
            <el-checkbox label="network">网络请求</el-checkbox>
            <el-checkbox label="file">文件读写</el-checkbox>
            <el-checkbox label="process">进程操作</el-checkbox>
            <el-checkbox label="reflection">反射调用</el-checkbox>
            <el-checkbox label="classloader">自定义类加载</el-checkbox>
            <el-checkbox label="thread">线程操作</el-checkbox>
          </el-checkbox-group>
        </el-card>
      </el-col>
    </el-row>
 
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card class="security-card">
          <template #header>
            <span>📝 允许的域名</span>
          </template>
          
          <div class="domain-list">
            <div
              v-for="(domain, index) in securitySettings.allowedDomains"
              :key="index"
              class="domain-item"
            >
              <el-input v-model="securitySettings.allowedDomains[index]" />
              <el-button
                type="danger"
                icon="Delete"
                size="small"
                @click="removeDomain(index)"
              />
            </div>
            <el-button type="primary" @click="addDomain">
              <el-icon><Plus /></el-icon>
              添加域名
            </el-button>
          </div>
        </el-card>
      </el-col>
 
      <el-col :span="12">
        <el-card class="security-card">
          <template #header>
            <span>⚠️ 安全日志</span>
          </template>
          
          <div class="security-logs">
            <el-empty v-if="securityLogs.length === 0" description="暂无安全事件" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="log in securityLogs.slice(0, 10)"
                :key="log.id"
                :timestamp="formatTime(log.timestamp)"
                :type="log.level === 'error' ? 'danger' : log.level === 'warn' ? 'warning' : 'info'"
              >
                <div class="log-content">
                  <div class="log-message">{{ log.message }}</div>
                  <div class="log-source">来源: {{ log.source }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-button link type="primary" @click="loadMoreLogs">
              查看更多日志
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
 
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="security-card">
          <template #header>
            <span>🔐 证书管理</span>
          </template>
          
          <el-table :data="certificates" style="width: 100%">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="issuer" label="颁发者" />
            <el-table-column prop="expires" label="过期时间">
              <template #default="{ row }">
                {{ formatDate(row.expires) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="viewCertificate(row)">
                  查看
                </el-button>
                <el-button size="small" type="danger" @click="deleteCertificate(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div style="margin-top: 20px">
            <el-button type="primary" @click="importCertificate">
              <el-icon><Upload /></el-icon>
              导入证书
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
 
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import axios from 'axios'
 
const saving = ref(false)
 
const securitySettings = ref({
  defaultLevel: 'standard',
  blockedOperations: ['process', 'reflection', 'classloader'],
  allowedDomains: ['*.moyue.com']
})
 
const securityLogs = ref<any[]>([])
 
const certificates = ref([
  {
    id: '1',
    name: '默认证书',
    issuer: 'Moyue Reader',
    expires: '2025-12-31T23:59:59'
  }
])
 
const saveSettings = async () => {
  saving.value = true
  try {
    await axios.put('/api/security/settings', securitySettings.value)
    ElMessage.success('设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
 
const loadSecurityLogs = async () => {
  try {
    const res = await axios.get('/api/security/logs')
    securityLogs.value = res.data
  } catch (error) {
    console.error('加载日志失败:', error)
  }
}
 
const loadMoreLogs = () => {
  ElMessage.info('功能开发中...')
}
 
const addDomain = () => {
  securitySettings.value.allowedDomains.push('')
}
 
const removeDomain = (index: number) => {
  securitySettings.value.allowedDomains.splice(index, 1)
}
 
const viewCertificate = (cert: any) => {
  ElMessageBox.alert(
    `名称: ${cert.name}\n颁发者: ${cert.issuer}\n过期时间: ${formatDate(cert.expires)}`,
    '证书详情',
    {
      confirmButtonText: '关闭'
    }
  )
}
 
const deleteCertificate = async (cert: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这个证书吗？', '警告', {
      type: 'warning'
    })
    certificates.value = certificates.value.filter(c => c.id !== cert.id)
    ElMessage.success('删除成功')
  } catch (error) {
    // 用户取消
  }
}
 
const importCertificate = () => {
  ElMessage.info('功能开发中...')
}
 
const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleString()
}
 
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}
 
onMounted(() => {
  loadSecurityLogs()
})
</script>
 
<style scoped>
.security {
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
 
.security-card {
  margin-bottom: 20px;
}
 
.level-description {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-top: 10px;
}
 
.level-description h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
}
 
.level-description ul {
  margin: 0;
  padding-left: 20px;
}
 
.level-description li {
  margin: 5px 0;
  font-size: 13px;
  color: #666;
}
 
.domain-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
 
.domain-item {
  display: flex;
  gap: 10px;
}
 
.security-logs {
  max-height: 400px;
  overflow-y: auto;
}
 
.log-content {
  font-size: 13px;
}
 
.log-message {
  margin-bottom: 5px;
}
 
.log-source {
  color: #999;
  font-size: 12px;
}
</style>
