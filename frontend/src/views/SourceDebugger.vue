import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MonacoEditor from '@/components/MonacoEditor.vue'
import axios from 'axios'
import { useSourceStore } from '@/stores/source'

<template>
  <div class="source-debugger">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="debugger-card">
          <template #header>
            <div class="card-header">
              <span>🔧 书源调试器</span>
              <el-button-group>
                <el-button size="small" @click="createSession" :loading="loading">
                  新建会话
                </el-button>
                <el-button size="small" @click="clearLogs">
                  清空日志
                </el-button>
              </el-button-group>
            </div>
          </template>
          
          <el-row :gutter="20">
            <!-- 左侧：代码编辑器 -->
            <el-col :span="14">
              <div class="editor-container">
                <div class="editor-tabs">
                  <el-tabs v-model="activeTab">
                    <el-tab-pane label="书源代码" name="code">
                      <MonacoEditor
                        v-model="sourceCode"
                        language="javascript"
                        height="400px"
                        :options="editorOptions"
                      />
                    </el-tab-pane>
                    <el-tab-pane label="HTML预览" name="html">
                      <el-input
                        v-model="htmlPreview"
                        type="textarea"
                        :rows="12"
                        placeholder="输入HTML内容进行规则测试"
                      />
                    </el-tab-pane>
                    <el-tab-pane label="JSON预览" name="json">
                      <el-input
                        v-model="jsonPreview"
                        type="textarea"
                        :rows="12"
                        placeholder="输入JSON内容进行规则测试"
                      />
                    </el-tab-pane>
                  </el-tabs>
                </div>
                
                <div class="function-bar">
                  <el-select v-model="selectedFunction" placeholder="选择函数" size="small">
                    <el-option label="search" value="search" />
                    <el-option label="getBookInfo" value="getBookInfo" />
                    <el-option label="getChapterList" value="getChapterList" />
                    <el-option label="getContent" value="getContent" />
                  </el-select>
                  
                  <el-input
                    v-model="functionArgs"
                    placeholder="参数（用逗号分隔）"
                    size="small"
                    style="width: 300px; margin: 0 10px"
                  />
                  
                  <el-button type="primary" size="small" @click="executeCode" :loading="executing">
                    执行
                  </el-button>
                </div>
              </div>
            </el-col>
            
            <!-- 右侧：日志和结果 -->
            <el-col :span="10">
              <div class="result-panel">
                <el-tabs v-model="resultTab">
                  <el-tab-pane label="执行结果" name="result">
                    <div class="result-content">
                      <pre>{{ executionResult }}</pre>
                    </div>
                  </el-tab-pane>
                  <el-tab-pane label="调试日志" name="logs">
                    <div class="logs-container" ref="logsContainer">
                      <div
                        v-for="(log, index) in logs"
                        :key="index"
                        :class="['log-item', log.level]"
                      >
                        <span class="log-time">{{ formatTime(log.timestamp) }}</span>
                        <span class="log-level" :class="log.level">[{{ log.level }}]</span>
                        <span class="log-message">{{ log.message }}</span>
                        <span v-if="log.data" class="log-data">{{ log.data }}</span>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card class="rule-tester">
          <template #header>
            <span>🧪 规则测试</span>
          </template>
          
          <el-form :inline="true" size="small">
            <el-form-item label="规则类型">
              <el-select v-model="ruleType">
                <el-option label="XPath" value="xpath" />
                <el-option label="CSS选择器" value="css" />
                <el-option label="正则表达式" value="regex" />
                <el-option label="JSONPath" value="json" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="规则" style="width: 300px">
              <el-input v-model="ruleText" placeholder="输入规则" />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="testRule">测试规则</el-button>
            </el-form-item>
          </el-form>
          
          <div class="rule-result">
            <h4>提取结果：</h4>
            <pre>{{ ruleResult }}</pre>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card class="source-info">
          <template #header>
            <span>📖 当前书源</span>
          </template>
          
          <el-form label-width="80px" size="small">
            <el-form-item label="书源">
              <el-select v-model="currentSourceId" filterable placeholder="选择书源">
                <el-option
                  v-for="source in sources"
                  :key="source.id"
                  :label="source.name"
                  :value="source.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          
          <div v-if="currentSource" class="source-detail">
            <p><strong>URL:</strong> {{ currentSource.url }}</p>
            <p><strong>分组:</strong> {{ currentSource.group }}</p>
            <p><strong>启用:</strong> {{ currentSource.enabled ? '是' : '否' }}</p>
            <p><strong>安全评级:</strong> 
              <el-rate
                v-model="currentSource.securityRating"
                disabled
                text-color="#ff9900"
              />
            </p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MonacoEditor from '@/components/MonacoEditor.vue'
import axios from 'axios'
import { useSourceStore } from '@/stores/source'

const sourceStore = useSourceStore()

// 状态
const loading = ref(false)
const executing = ref(false)
const sessionId = ref('')
const activeTab = ref('code')
const resultTab = ref('result')
const sourceCode = ref('')
const htmlPreview = ref('')
const jsonPreview = ref('')
const selectedFunction = ref('search')
const functionArgs = ref('')
const logs = ref<any[]>([])
const executionResult = ref('')
const currentSourceId = ref('')
const sources = ref<any[]>([])

// 规则测试
const ruleType = ref('xpath')
const ruleText = ref('')
const ruleResult = ref('')

const editorOptions = {
  minimap: { enabled: false },
  fontSize: 14,
  lineNumbers: 'on',
  automaticLayout: true
}

const currentSource = computed(() => {
  return sources.value.find(s => s.id === currentSourceId.value)
})

// 创建调试会话
const createSession = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/debug/session', null, {
      params: { sourceId: currentSourceId.value || undefined }
    })
    sessionId.value = res.data.sessionId
    ElMessage.success('调试会话已创建')
  } catch (error) {
    ElMessage.error('创建会话失败')
  } finally {
    loading.value = false
  }
}

// 执行代码
const executeCode = async () => {
  if (!sessionId.value) {
    ElMessage.warning('请先创建调试会话')
    return
  }
  
  executing.value = true
  try {
    const args = functionArgs.value.split(',').map(s => s.trim()).filter(Boolean)
    
    const res = await axios.post(`/api/debug/session/${sessionId.value}/execute`, {
      code: sourceCode.value,
      function: selectedFunction.value,
      args
    })
    
    executionResult.value = JSON.stringify(res.data.result, null, 2)
    
    // 刷新日志
    await fetchLogs()
  } catch (error) {
    ElMessage.error('执行失败')
  } finally {
    executing.value = false
  }
}

// 获取日志
const fetchLogs = async () => {
  if (!sessionId.value) return
  
  try {
    const res = await axios.get(`/api/debug/session/${sessionId.value}/logs`)
    logs.value = res.data
  } catch (error) {
    console.error('获取日志失败', error)
  }
}

// 清空日志
const clearLogs = async () => {
  if (!sessionId.value) {
    ElMessage.warning('没有活动的调试会话')
    return
  }
  
  try {
    await axios.delete(`/api/debug/session/${sessionId.value}/logs`)
    logs.value = []
    ElMessage.success('日志已清空')
  } catch (error) {
    ElMessage.error('清空日志失败')
  }
}

// 测试规则
const testRule = async () => {
  if (!ruleText.value) {
    ElMessage.warning('请输入规则')
    return
  }
  
  const testHtml = activeTab.value === 'html' ? htmlPreview.value : 
                   activeTab.value === 'json' ? jsonPreview.value : ''
  
  try {
    const res = await axios.post(`/api/debug/session/${sessionId.value}/test-rule`, {
      ruleType: ruleType.value,
      rule: ruleText.value,
      html: testHtml
    })
    
    ruleResult.value = JSON.stringify(res.data.extracted, null, 2)
  } catch (error) {
    ElMessage.error('测试规则失败')
  }
}

// 格式化时间
const formatTime = (timestamp: string) => {
  return new Date(timestamp).toLocaleTimeString()
}

// 监听会话变化，定时刷新日志
watch(sessionId, (newVal) => {
  if (newVal) {
    setInterval(fetchLogs, 2000)
  }
})

onMounted(async () => {
  await sourceStore.fetchSources()
  sources.value = sourceStore.sources
})
</script>

<style scoped>
.source-debugger {
  padding: 20px;
  height: calc(100vh - 100px);
  overflow-y: auto;
}

.debugger-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.function-bar {
  padding: 10px;
  background-color: #f5f7fa;
  border-top: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
}

.result-panel {
  height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.result-content {
  padding: 10px;
  height: 400px;
  overflow-y: auto;
  background-color: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', monospace;
  font-size: 12px;
}

.logs-container {
  height: 400px;
  overflow-y: auto;
  background-color: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', monospace;
  padding: 10px;
}

.log-item {
  padding: 2px 0;
  border-bottom: 1px solid #333;
  font-size: 12px;
}

.log-time {
  color: #808080;
  margin-right: 10px;
}

.log-level {
  margin-right: 10px;
  font-weight: bold;
}

.log-level.info { color: #4ec9b0; }
.log-level.warn { color: #f48771; }
.log-level.error { color: #f44747; }
.log-level.debug { color: #9cdcfe; }

.rule-tester {
  height: 300px;
}

.rule-result {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 150px;
  overflow-y: auto;
}

.rule-result pre {
  margin: 0;
  font-size: 12px;
}

.source-detail {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.source-detail p {
  margin: 5px 0;
  font-size: 13px;
}
</style>
