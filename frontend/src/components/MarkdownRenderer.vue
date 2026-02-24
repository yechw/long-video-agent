<template>
  <div class="markdown-content" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  content: string
}>()

// Configure marked with custom renderer for code highlighting
const renderer = new marked.Renderer()
renderer.code = ({ text, lang }: { text: string; lang?: string }) => {
  let highlighted: string
  if (lang && hljs.getLanguage(lang)) {
    highlighted = hljs.highlight(text, { language: lang }).value
  } else {
    highlighted = hljs.highlightAuto(text).value
  }
  return `<pre><code class="hljs">${highlighted}</code></pre>`
}

marked.setOptions({
  renderer,
  breaks: true,
  gfm: true
})

const renderedContent = computed(() => {
  return marked.parse(props.content) as string
})
</script>

<style>
.markdown-content {
  line-height: 1.8;
  color: #303133;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  margin: 16px 0 8px;
  color: #303133;
}

.markdown-content p {
  margin: 8px 0;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 24px;
  margin: 8px 0;
}

.markdown-content li {
  margin: 4px 0;
}

.markdown-content pre {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content code {
  font-family: 'Monaco', 'Consolas', monospace;
  font-size: 14px;
}

.markdown-content p code {
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #e96900;
}

.markdown-content blockquote {
  border-left: 4px solid #409eff;
  padding-left: 12px;
  margin: 12px 0;
  color: #606266;
}
</style>
