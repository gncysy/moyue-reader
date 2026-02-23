<template>
  <div ref="editorContainer" class="monaco-editor" :style="{ height, width }"></div>
</template>
 
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'
 
// 配置 worker
self.MonacoEnvironment = {
  getWorker: function (workerId, label) {
    if (label === 'json') {
      return new jsonWorker()
    }
    if (label === 'css' || label === 'scss' || label === 'less') {
      return new cssWorker()
    }
    if (label === 'html' || label === 'handlebars' || label === 'razor') {
      return new htmlWorker()
    }
    if (label === 'typescript' || label === 'javascript') {
      return new tsWorker()
    }
    return new editorWorker()
  }
}
 
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'javascript'
  },
  theme: {
    type: String,
    default: 'vs-dark'
  },
  height: {
    type: String,
    default: '400px'
  },
  width: {
    type: String,
    default: '100%'
  },
  options: {
    type: Object,
    default: () => ({})
  }
})
 
const emit = defineEmits(['update:modelValue'])
 
const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null
 
onMounted(() => {
  if (!editorContainer.value) return
  
  editor = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: props.language,
    theme: props.theme,
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14,
    ...props.options
  })
  
  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor?.getValue() || '')
  })
})
 
onBeforeUnmount(() => {
  editor?.dispose()
})
 
watch(() => props.modelValue, (newVal) => {
  if (editor && newVal !== editor.getValue()) {
    const position = editor.getPosition()
    editor.setValue(newVal)
    if (position) {
      editor.setPosition(position)
    }
  }
})
 
watch(() => props.language, (newVal) => {
  const model = editor?.getModel()
  if (model) {
    monaco.editor.setModelLanguage(model, newVal)
  }
})
 
watch(() => props.theme, (newVal) => {
  monaco.editor.setTheme(newVal)
})
</script>
 
<style scoped>
.monaco-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
</style>
