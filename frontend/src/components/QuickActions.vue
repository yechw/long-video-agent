<template>
  <div class="card">
    <div class="card-header">快捷操作</div>

    <div class="action-buttons">
      <el-button type="primary" @click="doSummarize" :loading="loading === 'summarize'">
        生成摘要
      </el-button>
      <el-button type="success" @click="doExtract" :loading="loading === 'extract'">
        提取概念
      </el-button>
      <el-button type="warning" @click="doQuotes" :loading="loading === 'quotes'">
        提取金句
      </el-button>
      <el-button @click="showSearchDialog = true">
        关键词搜索
      </el-button>
    </div>

    <div v-if="result" class="result-area">
      <el-divider />
      <MarkdownRenderer :content="result" />
    </div>

    <el-dialog v-model="showSearchDialog" title="关键词搜索" width="500px">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入关键词"
        @keyup.enter="doSearch"
      />
      <template #footer>
        <el-button @click="showSearchDialog = false">取消</el-button>
        <el-button type="primary" @click="doSearch" :loading="loading === 'search'">
          搜索
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { videoApi } from '../api/videoApi'
import MarkdownRenderer from './MarkdownRenderer.vue'

const props = defineProps<{
  subtitleContent: string
}>()

const emit = defineEmits<{
  (e: 'result', data: { type: string; data: unknown }): void
}>()

const loading = ref<string | null>(null)
const result = ref('')
const showSearchDialog = ref(false)
const searchKeyword = ref('')

async function doSummarize() {
  loading.value = 'summarize'
  result.value = ''
  try {
    const response = await videoApi.summarize(props.subtitleContent)
    if (response.success) {
      result.value = response.content
    } else {
      ElMessage.error(response.message || '生成失败')
    }
  } catch (error) {
    ElMessage.error('生成摘要失败')
  } finally {
    loading.value = null
  }
}

async function doExtract() {
  loading.value = 'extract'
  result.value = ''
  try {
    const response = await videoApi.extractConcepts(props.subtitleContent)
    if (response.success) {
      result.value = response.content
      emit('result', { type: 'concepts', data: response.content })
    } else {
      ElMessage.error(response.message || '提取失败')
    }
  } catch (error) {
    ElMessage.error('提取概念失败')
  } finally {
    loading.value = null
  }
}

async function doQuotes() {
  loading.value = 'quotes'
  result.value = ''
  try {
    const response = await videoApi.extractQuotes(props.subtitleContent)
    if (response.success) {
      result.value = response.content
    } else {
      ElMessage.error(response.message || '提取失败')
    }
  } catch (error) {
    ElMessage.error('提取金句失败')
  } finally {
    loading.value = null
  }
}

async function doSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }

  loading.value = 'search'
  showSearchDialog.value = false
  result.value = ''
  try {
    const response = await videoApi.searchKeyword({
      subtitleContent: props.subtitleContent,
      keyword: searchKeyword.value
    })
    if (response.success) {
      result.value = response.content
    } else {
      ElMessage.error(response.message || '搜索失败')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = null
  }
}
</script>

<style scoped>
.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result-area {
  margin-top: 16px;
}
</style>
