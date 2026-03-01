<template>
  <div class="card">
    <div class="card-header">智能问答</div>

    <div class="chat-history" ref="historyRef">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="message-label">{{ msg.role === 'user' ? '你' : 'AI' }}</div>
        <div class="message-content">
          <!-- 流式输出时显示纯文本，完成后渲染 Markdown -->
          <template v-if="msg.role === 'assistant' && msg.streaming">
            <pre class="streaming-text">{{ msg.content }}</pre>
          </template>
          <MarkdownRenderer v-else-if="msg.role === 'assistant'" :content="msg.content" />
          <template v-else>{{ msg.content }}</template>
          <span v-if="msg.streaming" class="cursor">|</span>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="question"
        type="textarea"
        :rows="2"
        placeholder="输入你的问题... (Ctrl+Enter 发送)"
        @keydown.enter.ctrl="sendQuestion"
        :disabled="streaming"
      />
      <div class="input-actions">
        <el-checkbox v-model="useStream">流式输出</el-checkbox>
        <div class="input-actions-right">
          <el-button
            v-if="question.trim()"
            @click="openOptimizer"
            :icon="Tools"
            circle
            title="优化 Prompt"
          />
          <el-button type="primary" @click="sendQuestion" :loading="streaming">
            发送
          </el-button>
        </div>
      </div>
    </div>

    <PromptOptimizer
      v-model="optimizerVisible"
      :original-prompt="question"
      @use="handleUseOptimizedPrompt"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'
import { videoApi } from '../api/videoApi'
import MarkdownRenderer from './MarkdownRenderer.vue'
import PromptOptimizer from './PromptOptimizer.vue'

const props = defineProps<{
  subtitleContent: string
}>()

interface Message {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

const question = ref('')
const messages = ref<Message[]>([])
const streaming = ref(false)
const useStream = ref(true)
const historyRef = ref<HTMLElement | null>(null)
const optimizerVisible = ref(false)

const openOptimizer = () => {
  if (!question.value.trim()) {
    ElMessage.warning('请先输入 Prompt')
    return
  }
  optimizerVisible.value = true
}

const handleUseOptimizedPrompt = (optimizedPrompt: string) => {
  question.value = optimizedPrompt
  ElMessage.success('已使用优化后的 Prompt')
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
// Variable to store stream cleanup function (available for future use)
const _stopStream: { value: (() => void) | null } = { value: null }

function getMessage(msgIndex: number): Message | undefined {
  return messages.value[msgIndex]
}

function scrollToBottom() {
  nextTick(() => {
    if (historyRef.value) {
      historyRef.value.scrollTop = historyRef.value.scrollHeight
    }
  })
}

async function sendQuestion() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const userQuestion = question.value
  question.value = ''

  messages.value.push({ role: 'user', content: userQuestion })
  scrollToBottom()

  if (useStream.value) {
    await sendStreamQuestion(userQuestion)
  } else {
    await sendNormalQuestion(userQuestion)
  }
}

async function sendNormalQuestion(q: string) {
  streaming.value = true
  try {
    const response = await videoApi.smartAsk({
      subtitleContent: props.subtitleContent,
      question: q
    })
    messages.value.push({ role: 'assistant', content: response.content })
  } catch (error) {
    ElMessage.error('问答失败')
    messages.value.push({ role: 'assistant', content: '抱歉，发生了错误' })
  } finally {
    streaming.value = false
    scrollToBottom()
  }
}

async function sendStreamQuestion(q: string) {
  streaming.value = true

  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const msgIndex = messages.value.length - 1

  _stopStream.value = videoApi.streamAsk(
    props.subtitleContent,
    q,
    (chunk) => {
      const currentMsg = messages.value[msgIndex]
      if (currentMsg) {
        messages.value[msgIndex] = {
          ...currentMsg,
          content: currentMsg.content + chunk
        }
      }
      scrollToBottom()
    },
    (error) => {
      messages.value[msgIndex] = {
        ...messages.value[msgIndex],
        content: '错误: ' + error,
        streaming: false
      }
      streaming.value = false
    },
    () => {
      messages.value[msgIndex] = {
        ...messages.value[msgIndex],
        streaming: false
      }
      streaming.value = false
    }
  )
}
</script>

<style scoped>
.chat-history {
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.message {
  margin-bottom: 16px;
}

.message.user {
  text-align: right;
}

.message-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-content {
  display: inline-block;
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  text-align: left;
}

.message.user .message-content {
  background: #409eff;
  color: white;
}

.message.assistant .message-content {
  background: white;
  border: 1px solid #e4e7ed;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.streaming-text {
  margin: 0;
  font-family: inherit;
  font-size: inherit;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
