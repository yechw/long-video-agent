<template>
  <div class="app-container">
    <h1 class="app-title">长视频智能分析助手</h1>

    <FileUpload @subtitle-loaded="onSubtitleLoaded" />

    <template v-if="subtitleContent">
      <div class="card">
        <div class="card-header">字幕信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ fileName }}</el-descriptions-item>
          <el-descriptions-item label="字符数">{{ charCount }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <QuickActions :subtitle-content="subtitleContent" @result="onActionResult" />

      <ChatPanel :subtitle-content="subtitleContent" />

      <ConceptTimeline v-if="concepts.length > 0" :concepts="concepts" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import FileUpload from './components/FileUpload.vue'
import QuickActions from './components/QuickActions.vue'
import ChatPanel from './components/ChatPanel.vue'
import ConceptTimeline from './components/ConceptTimeline.vue'
import type { Concept } from './types'

const subtitleContent = ref('')
const fileName = ref('')
const charCount = ref(0)
const concepts = ref<Concept[]>([])

function onSubtitleLoaded(data: { content: string; name: string; count: number }) {
  subtitleContent.value = data.content
  fileName.value = data.name
  charCount.value = data.count
  concepts.value = []
}

function onActionResult(result: { type: string; data: unknown }) {
  if (result.type === 'concepts') {
    try {
      // 从 JSON 响应中提取概念数组
      const jsonStr = result.data as string
      const start = jsonStr.indexOf('[')
      const end = jsonStr.lastIndexOf(']')
      if (start >= 0 && end > start) {
        concepts.value = JSON.parse(jsonStr.substring(start, end + 1))
      }
    } catch {
      concepts.value = []
    }
  }
}
</script>

<style scoped>
.app-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.app-title {
  text-align: center;
  color: #303133;
  margin-bottom: 24px;
}
</style>
