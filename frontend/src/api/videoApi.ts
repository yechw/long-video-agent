import type { UploadResponse, VideoResponse, SmartAskResponse, ChatRequest, SearchRequest } from '../types'

const BASE_URL = '/api'

async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  return response.json()
}

async function postText<T>(url: string, body: string): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body
  })
  return response.json()
}

export const videoApi = {
  // 上传文件
  upload: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(`${BASE_URL}/upload`, {
      method: 'POST',
      body: formData
    })
    return response.json()
  },

  // 获取示例字幕
  getSampleSubtitle: async (): Promise<UploadResponse> => {
    return postText<UploadResponse>(`${BASE_URL}/upload/content`, '')
  },

  // 生成摘要
  summarize: async (subtitleContent: string): Promise<VideoResponse> => {
    return postText<VideoResponse>(`${BASE_URL}/summarize`, subtitleContent)
  },

  // 基础问答
  chat: async (request: ChatRequest): Promise<VideoResponse> => {
    return postJson<VideoResponse>(`${BASE_URL}/chat`, request)
  },

  // 提取概念
  extractConcepts: async (subtitleContent: string): Promise<VideoResponse> => {
    return postText<VideoResponse>(`${BASE_URL}/extract`, subtitleContent)
  },

  // 提取金句
  extractQuotes: async (subtitleContent: string): Promise<VideoResponse> => {
    return postText<VideoResponse>(`${BASE_URL}/quotes`, subtitleContent)
  },

  // 关键词搜索
  searchKeyword: async (request: SearchRequest): Promise<VideoResponse> => {
    return postJson<VideoResponse>(`${BASE_URL}/search`, request)
  },

  // 智能问答
  smartAsk: async (request: ChatRequest, debug = false): Promise<SmartAskResponse> => {
    const url = `${BASE_URL}/ask${debug ? '?debug=true' : ''}`
    return postJson<SmartAskResponse>(url, request)
  },

  // 流式问答（POST 请求，支持长字幕内容）
  streamAsk: (
    subtitleContent: string,
    question: string,
    onMessage: (chunk: string) => void,
    onError: (error: string) => void,
    onComplete: () => void
  ): (() => void) => {
    let aborted = false

    fetch(`${BASE_URL}/stream/ask`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ subtitleContent, question })
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error('No response body')
        }

        const decoder = new TextDecoder()

        while (!aborted) {
          const { done, value } = await reader.read()
          if (done) break

          const chunk = decoder.decode(value, { stream: true })
          // SSE 格式: "data:内容\n\n"
          const lines = chunk.split('\n')
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.slice(5)
              if (data === '[DONE]') {
                onComplete()
              } else {
                onMessage(data)
              }
            }
          }
        }
      })
      .catch((error) => {
        if (!aborted) {
          onError(error.message || '连接失败')
        }
      })

    return () => {
      aborted = true
    }
  }
}
