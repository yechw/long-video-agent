# Vue 前端改造设计文档

> 日期：2026-02-24（更新）
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
│  │  VideoController (Thymeleaf - 保留)              │    │
│  │  VideoApiController (REST API - 新增)            │    │
│  │  VideoService / VideoServiceImpl (保持不变)      │    │
│  │  IntentClassificationService (保持不变)          │    │
│  │  PromptTemplateService (保持不变)                │    │
│  │  Spring AI Alibaba (LLM 调用)                    │    │
│  │  Caffeine Cache (意图缓存)                       │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## 后端设计

### 现有代码结构（main 分支）

```
src/main/java/com/example/videoagent/
├── VideoAgentApplication.java
├── config/
│   ├── PromptConstants.java           # Prompt 常量
│   ├── PromptDefinition.java          # Prompt 定义
│   ├── PromptVersionConfig.java       # Prompt 版本管理
│   └── VersionInfo.java               # 版本信息
├── controller/
│   └── VideoController.java           # Thymeleaf 控制器（保留）
├── dto/
│   ├── ChatRequest.java               # 问答请求
│   ├── Concept.java                   # 知识概念
│   ├── IntentResult.java              # 意图分类结果
│   ├── SmartAskResponse.java          # 智能问答响应
│   └── VideoResponse.java             # 通用响应
├── enums/
│   └── UserIntent.java                # 用户意图枚举
└── service/
    ├── IntentClassificationService.java  # 意图分类
    ├── PromptTemplateService.java        # Prompt 模板
    ├── VideoService.java                 # 服务接口
    └── VideoServiceImpl.java             # 服务实现
```

### 新增 REST API 控制器

创建 `VideoApiController.java`，提供 `/api/*` REST 端点。

### API 端点设计

| 方法 | 路径 | 请求体 | 返回类型 | 说明 |
|------|------|--------|----------|------|
| POST | `/api/upload` | MultipartFile | `VideoResponse` | 上传字幕 |
| POST | `/api/upload/content` | String (text/plain) | `VideoResponse` | 获取示例字幕 |
| POST | `/api/summarize` | String (text/plain) | `VideoResponse` | 生成摘要 |
| POST | `/api/chat` | `ChatRequest` (JSON) | `VideoResponse` | 基础问答 |
| POST | `/api/extract` | String (text/plain) | `VideoResponse` | 提取概念 |
| POST | `/api/quotes` | String (text/plain) | `VideoResponse` | 提取金句 |
| POST | `/api/search` | `SearchRequest` (JSON) | `VideoResponse` | 关键词搜索 |
| POST | `/api/ask` | `ChatRequest` (JSON) | `SmartAskResponse` | 智能问答 |
| GET | `/api/stream/ask` | Query Params | `SseEmitter` | 流式问答 |

### VideoApiController 设计

```java
@RestController
@RequestMapping("/api")
public class VideoApiController {

    private final VideoService videoService;
    private final IntentClassificationService intentClassificationService;

    // POST /api/upload - 上传字幕文件
    @PostMapping("/upload")
    public VideoResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "useSample", required = false) Boolean useSample);

    // POST /api/upload/content - 获取示例字幕或上传内容
    @PostMapping("/upload/content")
    public VideoResponse uploadWithContent(@RequestBody(required = false) String content);

    // POST /api/summarize - 生成摘要
    @PostMapping("/summarize")
    public VideoResponse summarize(@RequestBody String subtitleContent);

    // POST /api/chat - 基础问答
    @PostMapping("/chat")
    public VideoResponse chat(@RequestBody ChatRequest request);

    // POST /api/extract - 提取知识概念
    @PostMapping("/extract")
    public VideoResponse extractConcepts(@RequestBody String subtitleContent);

    // POST /api/quotes - 提取金句
    @PostMapping("/quotes")
    public VideoResponse extractQuotes(@RequestBody String subtitleContent);

    // POST /api/search - 关键词搜索
    @PostMapping("/search")
    public VideoResponse searchKeyword(@RequestBody SearchRequest request);

    // POST /api/ask - 智能问答
    @PostMapping("/ask")
    public SmartAskResponse smartAsk(
            @RequestBody ChatRequest request,
            @RequestParam(value = "debug", required = false, defaultValue = "false") Boolean debug);

    // GET /api/stream/ask - 流式问答（SSE）
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter smartAskStream(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question);
}
```

### 需要新增的 DTO

```java
// SearchRequest.java - 搜索请求
public class SearchRequest {
    private String subtitleContent;
    private String keyword;
    // getters, setters
}
```

### 需要扩展的 DTO

```java
// ChatRequest.java - 需要添加 subtitleContent 字段
public class ChatRequest {
    private String subtitleContent;  // 新增
    private String question;
    // getters, setters
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
│   │   └── videoApi.ts              # API 调用封装
│   ├── components/
│   │   ├── FileUpload.vue           # 文件上传
│   │   ├── QuickActions.vue         # 快捷操作
│   │   ├── ChatPanel.vue            # 问答面板（含流式）
│   │   ├── ConceptTimeline.vue      # 概念时间轴
│   │   └── MarkdownRenderer.vue     # Markdown 渲染
│   ├── types/
│   │   └── index.ts                 # TypeScript 类型定义
│   └── style.css
└── dist/
```

### 组件设计

| 组件 | 功能 |
|------|------|
| **FileUpload.vue** | 字幕文件上传，支持拖拽，支持示例字幕 |
| **QuickActions.vue** | 快捷操作按钮（摘要、概念、金句、搜索） |
| **ChatPanel.vue** | 问答对话面板，支持流式输出 |
| **ConceptTimeline.vue** | 概念时间轴可视化 |
| **MarkdownRenderer.vue** | Markdown 渲染（marked + highlight.js） |

### TypeScript 类型定义

```typescript
// types/index.ts

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
  intent?: string       // 调试模式时返回
  confidence?: number   // 调试模式时返回
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
```

### API 调用层

```typescript
// api/videoApi.ts
import type { UploadResponse, VideoResponse, SmartAskResponse, ChatRequest, SearchRequest } from '../types'

const BASE_URL = '/api'

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
    const response = await fetch(`${BASE_URL}/upload/content`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: ''
    })
    return response.json()
  },

  // 生成摘要
  summarize: async (subtitleContent: string): Promise<VideoResponse> => {
    const response = await fetch(`${BASE_URL}/summarize`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: subtitleContent
    })
    return response.json()
  },

  // 基础问答
  chat: async (request: ChatRequest): Promise<VideoResponse> => {
    const response = await fetch(`${BASE_URL}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    })
    return response.json()
  },

  // 提取概念
  extractConcepts: async (subtitleContent: string): Promise<VideoResponse> => {
    const response = await fetch(`${BASE_URL}/extract`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: subtitleContent
    })
    return response.json()
  },

  // 提取金句
  extractQuotes: async (subtitleContent: string): Promise<VideoResponse> => {
    const response = await fetch(`${BASE_URL}/quotes`, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: subtitleContent
    })
    return response.json()
  },

  // 关键词搜索
  searchKeyword: async (request: SearchRequest): Promise<VideoResponse> => {
    const response = await fetch(`${BASE_URL}/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    })
    return response.json()
  },

  // 智能问答
  smartAsk: async (request: ChatRequest, debug = false): Promise<SmartAskResponse> => {
    const url = `${BASE_URL}/ask${debug ? '?debug=true' : ''}`
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    })
    return response.json()
  },

  // 流式问答
  streamAsk: (
    subtitleContent: string,
    question: string,
    onMessage: (chunk: string) => void,
    onError: (error: string) => void,
    onComplete: () => void
  ): (() => void) => {
    const params = new URLSearchParams({ subtitleContent, question })
    const eventSource = new EventSource(`${BASE_URL}/stream/ask?${params}`)

    eventSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        eventSource.close()
        onComplete()
      } else {
        onMessage(event.data)
      }
    }

    eventSource.onerror = () => {
      onError('连接失败')
      eventSource.close()
    }

    return () => eventSource.close()
  }
}
```

## Vite 配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
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

## 部署流程

### 开发阶段

- 后端：`mvn spring-boot:run` (端口 8080)
- 前端：`npm run dev` (端口 5173)
- Vite 代理 `/api/*` 到 `localhost:8080`

### 生产构建

使用 `frontend-maven-plugin` 自动化构建：

1. `mvn clean package` 自动执行：
   - 安装 Node.js
   - `npm install`
   - `npm run build`
   - 复制 `dist/*` 到 `target/classes/static/`
2. `java -jar target/video-agent-0.0.1-SNAPSHOT.jar`
3. 访问 `http://localhost:8080`

### Maven 配置

```xml
<!-- frontend-maven-plugin -->
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install node and npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v20.10.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals><goal>npm</goal></goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <goals><goal>npm</goal></goals>
            <phase>prepare-package</phase>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- 复制前端构建产物 -->
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend-build</id>
            <phase>prepare-package</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                <resources>
                    <resource>
                        <directory>frontend/dist</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### SPA 路由配置

```java
// SpaConfig.java
@Configuration
public class SpaConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
```

## 实施阶段

### Phase 1: 后端 API 层

1. 创建 `VideoApiController.java`
2. 创建 `SearchRequest.java` DTO
3. 扩展 `ChatRequest.java`（添加 subtitleContent）
4. 实现所有 `/api/*` 端点
5. 测试 API 端点

### Phase 2: 前端 Vue 开发

1. 创建 Vue 项目 (`npm create vite@latest frontend -- --template vue-ts`)
2. 安装依赖 (`element-plus`, `marked`, `highlight.js`)
3. 创建类型定义
4. 创建 API 调用层
5. 实现组件
6. 联调测试

### Phase 3: 整合部署

1. 配置 Maven 前端构建插件
2. 配置 SPA 路由回退
3. 验证单 JAR 部署
4. （可选）移除 Thymeleaf 依赖

## 功能清单

| 功能 | Thymeleaf | REST API | Vue |
|------|-----------|----------|-----|
| 字幕文件上传 | ✅ | 🔄 | ⬜ |
| 生成全局摘要 | ✅ | 🔄 | ⬜ |
| 提取知识概念 | ✅ | 🔄 | ⬜ |
| 提取金句 | ✅ | 🔄 | ⬜ |
| 关键词搜索 | ✅ | 🔄 | ⬜ |
| 智能问答（意图识别） | ✅ | 🔄 | ⬜ |
| 流式输出（SSE） | ✅ | 🔄 | ⬜ |
| Prompt 版本管理 | ✅ | 🔄 | ⬜ |

图例：✅ 已实现 | 🔄 需要实现 | ⬜ 待开发

## 注意事项

1. **保持兼容**：保留 VideoController（Thymeleaf）作为备用，新 API 独立运行
2. **状态管理**：前端使用 Vue 3 Composition API 管理字幕内容状态
3. **错误处理**：API 统一返回 `success` 字段标识成功/失败
4. **流式输出**：SSE 连接需要处理超时和错误情况
5. **CORS**：开发阶段通过 Vite 代理解决，生产环境同源无需处理
