# Vue 前端改造设计文档

> 日期：2026-02-22
> 状态：已批准

## 概述

将 LongVideoAgent 项目从 Thymeleaf 服务端渲染改造为 Vue 3 SPA 前端，后端新增 REST API 层，最终实现单体 JAR 部署。

## 需求决策

| 项目 | 选择 |
|------|------|
| 前端框架 | Vue 3 + TypeScript |
| UI 组件库 | Element Plus |
| 构建工具 | Vite |
| 后端架构 | Spring MVC（保持不变） |
| 部署方式 | 单体部署（Vue 打包到 static/） |
| 功能范围 | 全部保留（1:1 迁移） |

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    单体 JAR 部署                          │
├─────────────────────────────────────────────────────────┤
│  Vue 3 SPA (打包后)                                      │
│  ┌─────────────────────────────────────────────────┐    │
│  │  Element Plus + TypeScript + Vite               │    │
│  │  - 文件上传组件                                   │    │
│  │  - 问答对话组件                                   │    │
│  │  - 流式输出组件                                   │    │
│  │  - 概念时间轴组件                                 │    │
│  └─────────────────────────────────────────────────┘    │
│                         ↓ REST API / SSE                │
├─────────────────────────────────────────────────────────┤
│  Spring MVC (保持不变)                                   │
│  ┌─────────────────────────────────────────────────┐    │
│  │  VideoApiController (新增)                       │    │
│  │  VideoService (保持不变)                         │    │
│  │  Spring AI Alibaba (LLM 调用)                    │    │
│  │  Caffeine Cache (意图缓存)                       │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## 后端设计

### 项目结构调整

```
src/main/java/com/example/videoagent/
├── VideoAgentApplication.java
├── config/
│   └── PromptConstants.java       # 保留
├── controller/
│   ├── VideoController.java       # 保留（过渡期）
│   └── VideoApiController.java    # 新增
├── dto/                           # 保留
├── service/                       # 保留
└── enums/                         # 保留
```

### API 端点设计

| 方法 | 路径 | 返回类型 | 说明 |
|------|------|----------|------|
| POST | `/api/upload` | `VideoResponse` | 上传字幕 |
| POST | `/api/summarize` | `VideoResponse` | 生成摘要 |
| POST | `/api/chat` | `VideoResponse` | 基础问答 |
| POST | `/api/extract` | `VideoResponse` | 提取概念 |
| POST | `/api/quotes` | `VideoResponse` | 提取金句 |
| POST | `/api/search` | `VideoResponse` | 关键词搜索 |
| POST | `/api/ask` | `SmartAskResponse` | 智能问答 |
| GET | `/api/stream/ask` | `SseEmitter` | 流式问答 |

### VideoApiController 示例

```java
@RestController
@RequestMapping("/api")
public class VideoApiController {

    private final VideoService videoService;

    @PostMapping("/upload")
    public VideoResponse upload(@RequestParam("file") MultipartFile file) { ... }

    @PostMapping("/summarize")
    public VideoResponse summarize() { ... }

    @GetMapping("/stream/ask")
    public SseEmitter smartAskStream(@RequestParam String question) { ... }
}
```

## 前端设计

### 项目结构

```
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/
│   │   └── videoApi.ts
│   ├── components/
│   │   ├── FileUpload.vue
│   │   ├── QuickActions.vue
│   │   ├── ChatPanel.vue
│   │   ├── StreamMessage.vue
│   │   ├── ConceptTimeline.vue
│   │   └── MarkdownRenderer.vue
│   ├── composables/
│   │   ├── useVideoApi.ts
│   │   └── useMarkdown.ts
│   ├── types/
│   │   └── index.ts
│   └── style.css
└── dist/
```

### 组件设计

- **FileUpload.vue**: 字幕文件上传，支持拖拽
- **QuickActions.vue**: 快捷操作按钮（摘要、概念、金句、搜索）
- **ChatPanel.vue**: 问答对话面板
- **StreamMessage.vue**: SSE 流式消息显示
- **ConceptTimeline.vue**: 概念时间轴可视化
- **MarkdownRenderer.vue**: Markdown 渲染（使用 marked + highlight.js）

### API 调用层

```typescript
// api/videoApi.ts
const BASE_URL = '/api';

export const videoApi = {
  upload: (file: File): Promise<UploadResponse> => { ... },
  summarize: (): Promise<VideoResponse> => { ... },
  chat: (question: string): Promise<VideoResponse> => { ... },
  extract: (): Promise<ConceptExtractResponse> => { ... },
  smartAsk: (question: string): Promise<SmartAskResponse> => { ... },

  streamAsk: (question: string, onMessage: (chunk: string) => void) => {
    const eventSource = new EventSource(`/api/stream/ask?question=...`);
    eventSource.onmessage = (event) => onMessage(event.data);
    return () => eventSource.close();
  }
};
```

## API 契约

### DTO 定义

```typescript
// 上传响应
interface UploadResponse {
  success: boolean;
  message: string;
  filename?: string;
}

// 通用 AI 响应
interface VideoResponse {
  success: boolean;
  content: string;
  error?: string;
}

// 智能问答响应
interface SmartAskResponse {
  success: boolean;
  content: string;
  intent?: {
    type: string;
    confidence: number;
  };
}

// 概念提取响应
interface ConceptExtractResponse {
  success: boolean;
  concepts: Concept[];
}

interface Concept {
  name: string;
  description: string;
  startTime: string;
  endTime: string;
}
```

## 部署流程

### 开发阶段

- 后端：`mvn spring-boot:run` (端口 8080)
- 前端：`npm run dev` (端口 5173)
- Vite 代理 `/api/*` 到 `localhost:8080`

### Vite 配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
})
```

### 生产构建

1. `cd frontend && npm run build`
2. 复制 `dist/*` 到 `src/main/resources/static/`
3. `mvn clean package`
4. `java -jar target/video-agent.jar`

## 实施阶段

### Phase 1: 后端 API 层

- 新建 `VideoApiController`
- 添加 `/api/*` 端点
- 保留原有 Controller（过渡期）

### Phase 2: 前端 Vue 开发

- 创建 `frontend/` 项目
- 实现各组件
- 配置 Vite 代理
- 联调测试

### Phase 3: 整合部署

- 配置 Maven 前端构建插件
- 移除 Thymeleaf 依赖
- 单 JAR 部署验证

## 功能清单

- [x] 字幕文件上传
- [x] 生成全局摘要
- [x] 提取知识概念（带时间轴）
- [x] 智能问答（意图自动识别）
- [x] 流式输出（SSE）
- [x] 深度分析模式（/deep 前缀）
- [x] 提取金句
- [x] 关键词搜索
