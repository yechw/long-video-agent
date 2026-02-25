// 上传响应
export interface UploadResponse {
  success: boolean
  message: string
  fileName?: string
  charCount?: number
  content?: string
}

// 通用 AI 响应
export interface VideoResponse {
  success: boolean
  content: string
  message?: string
}

// 智能问答响应
export interface SmartAskResponse {
  content: string
  intent?: string
  confidence?: number
}

// 知识概念
export interface Concept {
  timestampFrom: string
  timestampTo: string
  concept: string
  description: string
}

// 聊天请求
export interface ChatRequest {
  subtitleContent: string
  question: string
}

// 搜索请求
export interface SearchRequest {
  subtitleContent: string
  keyword: string
}
