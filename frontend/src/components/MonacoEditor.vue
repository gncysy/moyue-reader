<template>
  <div ref="editorContainer" class="monaco-editor" :style="{ height, width }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

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
    editor.setValue(newVal)
  }
})

watch(() => props.language, (newVal) => {
  monaco.editor.setModelLanguage(editor?.getModel()!, newVal)
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
