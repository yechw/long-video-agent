<template>
  <div class="card">
    <div class="card-header">上传字幕文件</div>

    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
      accept=".txt,.srt"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">支持 .txt, .srt 格式的字幕文件</div>
      </template>
    </el-upload>

    <div class="upload-actions">
      <el-button type="primary" @click="useSample" :loading="loading">
        使用示例字幕
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { videoApi } from '../api/videoApi'

const emit = defineEmits<{
  (e: 'subtitle-loaded', data: { content: string; name: string; count: number }): void
}>()

const loading = ref(false)

async function handleFileChange(file: any) {
  if (!file) return

  loading.value = true
  try {
    const response = await videoApi.upload(file.raw)
    if (response.success) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const content = e.target?.result as string
        emit('subtitle-loaded', {
          content,
          name: response.fileName || file.name,
          count: response.charCount || content.length
        })
        ElMessage.success('字幕上传成功')
      }
      reader.readAsText(file.raw)
    } else {
      ElMessage.error(response.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    loading.value = false
  }
}

async function useSample() {
  loading.value = true
  try {
    const response = await videoApi.getSampleSubtitle()
    if (response.success && response.content) {
      emit('subtitle-loaded', {
        content: response.content,
        name: response.fileName || 'sample.srt (示例)',
        count: response.charCount || response.content.length
      })
      ElMessage.success('示例字幕加载成功')
    } else {
      ElMessage.error(response.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error('加载示例字幕失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.upload-area {
  width: 100%;
}

.upload-actions {
  margin-top: 16px;
  text-align: center;
}
</style>
