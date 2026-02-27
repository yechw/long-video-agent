# Vue 前端改造实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 LongVideoAgent 从 Thymeleaf 服务端渲染改造为 Vue 3 SPA，后端新增 REST API 层。

**Architecture:** 后端新建 VideoApiController 提供 `/api/*` REST 接口，保持原有 Service 不变；前端使用 Vue 3 + TypeScript + Element Plus 构建 SPA，打包后放入 static/ 目录实现单体部署。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vite, Spring MVC, SseEmitter

---

## Phase 1: 后端 API 层

### Task 1.1: 扩展 ChatRequest DTO

**Files:**
- Modify: `src/main/java/com/example/videoagent/dto/ChatRequest.java`

**Step 1: 添加 subtitleContent 字段**

```java
package com.example.videoagent.dto;

/**
 * 问答请求参数
 */
public class ChatRequest {

    private String subtitleContent;  // 新增：字幕内容
    private String question;

    public ChatRequest() {}

    public ChatRequest(String subtitleContent, String question) {
        this.subtitleContent = subtitleContent;
        this.question = question;
    }

    public String getSubtitleContent() {
        return subtitleContent;
    }

    public void setSubtitleContent(String subtitleContent) {
        this.subtitleContent = subtitleContent;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/dto/ChatRequest.java
git commit -m "feat(dto): add subtitleContent to ChatRequest for REST API"
```

---

### Task 1.2: 创建 SearchRequest DTO

**Files:**
- Create: `src/main/java/com/example/videoagent/dto/SearchRequest.java`

**Step 1: 创建 SearchRequest 类**

```java
package com.example.videoagent.dto;

/**
 * 搜索请求参数
 */
public class SearchRequest {

    private String subtitleContent;
    private String keyword;

    public SearchRequest() {}

    public SearchRequest(String subtitleContent, String keyword) {
        this.subtitleContent = subtitleContent;
        this.keyword = keyword;
    }

    public String getSubtitleContent() {
        return subtitleContent;
    }

    public void setSubtitleContent(String subtitleContent) {
        this.subtitleContent = subtitleContent;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/dto/SearchRequest.java
git commit -m "feat(dto): add SearchRequest for keyword search API"
```

---

### Task 1.3: 创建 VideoApiController 骨架

**Files:**
- Create: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 创建 Controller 类**

```java
package com.example.videoagent.controller;

import com.example.videoagent.dto.ChatRequest;
import com.example.videoagent.dto.Concept;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.SearchRequest;
import com.example.videoagent.dto.SmartAskResponse;
import com.example.videoagent.dto.VideoResponse;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoApiController {

    private static final Logger log = LoggerFactory.getLogger(VideoApiController.class);

    private final VideoService videoService;
    private final IntentClassificationService intentClassificationService;

    public VideoApiController(VideoService videoService,
                              IntentClassificationService intentClassificationService) {
        this.videoService = videoService;
        this.intentClassificationService = intentClassificationService;
    }

    // 端点将在后续任务中添加
}
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat(api): add VideoApiController skeleton"
```

---

### Task 1.4: 添加上传 API 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 添加 upload 和 uploadWithContent 端点**

在 VideoApiController 类中添加：

```java
    /**
     * 上传字幕文件
     */
    @PostMapping("/upload")
    public VideoResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "useSample", required = false) Boolean useSample) throws IOException {

        String content;
        String fileName;

        if (Boolean.TRUE.equals(useSample)) {
            content = loadSampleSubtitle();
            fileName = "sample.srt (示例)";
        } else {
            if (file.isEmpty()) {
                return VideoResponse.error("请选择文件或使用示例字幕");
            }
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
            fileName = file.getOriginalFilename();
        }

        return VideoResponse.uploadSuccess(fileName, content.length());
    }

    /**
     * 获取上传的字幕内容（用于示例字幕或直接提交内容）
     */
    @PostMapping("/upload/content")
    public VideoResponse uploadWithContent(@RequestBody(required = false) String content) {
        if (content == null || content.isEmpty()) {
            content = loadSampleSubtitle();
        }
        return VideoResponse.success("字幕加载成功", content);
    }

    private String loadSampleSubtitle() {
        return """
                1
                00:00:00,000 --> 00:00:05,000
                大家好，欢迎来到今天的 AI 工程课程。

                2
                00:00:05,000 --> 00:00:12,000
                今天我们要讨论的是提示工程（Prompt Engineering），
                这是构建 AI 应用的核心技能之一。

                3
                00:00:12,000 --> 00:00:20,000
                提示工程的核心在于如何有效地与大型语言模型沟通，
                让模型生成我们期望的输出。

                4
                00:00:20,000 --> 00:00:30,000
                一个好的提示包含三个关键要素：
                任务描述、示例、以及具体的任务指令。

                5
                00:00:30,000 --> 00:00:40,000
                系统提示（System Prompt）用于设定 AI 的角色和行为边界，
                而用户提示（User Prompt）则包含具体的任务内容。

                6
                00:00:40,000 --> 00:00:50,000
                在处理长文本时，有一个重要的技巧：
                将关键指令放在提示的末尾，这样可以防止指令被长文本冲淡。

                7
                00:00:50,000 --> 00:01:00,000
                另外，为了防止模型产生幻觉，
                我们需要在系统提示中明确限制模型只能基于提供的内容回答。

                8
                00:01:00,000 --> 00:01:10,000
                Few-shot prompting 是另一个强大的技术，
                通过提供几个示例，可以让模型快速学习特定的输出格式。

                9
                00:01:10,000 --> 00:01:20,000
                总结一下，提示工程的关键点包括：
                清晰的指令、合适的示例、以及明确的约束条件。

                10
                00:01:20,000 --> 00:01:30,000
                下一节课，我们将讨论如何使用 RAG 技术
                来增强 AI 应用的知识检索能力。
                """;
    }
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat(api): add upload endpoints"
```

---

### Task 1.5: 添加摘要和问答 API 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 添加 summarize 和 chat 端点**

```java
    /**
     * 生成视频摘要
     */
    @PostMapping("/summarize")
    public VideoResponse summarize(@RequestBody String subtitleContent) {
        try {
            String summary = videoService.summarize(subtitleContent);
            return VideoResponse.success(summary);
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return VideoResponse.error("生成摘要失败: " + e.getMessage());
        }
    }

    /**
     * 基础问答
     */
    @PostMapping("/chat")
    public VideoResponse chat(@RequestBody ChatRequest request) {
        try {
            String answer = videoService.chat(request.getSubtitleContent(), request.getQuestion());
            return VideoResponse.success(answer);
        } catch (Exception e) {
            log.error("问答失败", e);
            return VideoResponse.error("问答失败: " + e.getMessage());
        }
    }
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat(api): add summarize and chat endpoints"
```

---

### Task 1.6: 添加概念提取、金句、搜索 API 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 添加 extract, quotes, search 端点**

```java
    /**
     * 提取知识概念
     */
    @PostMapping("/extract")
    public VideoResponse extractConcepts(@RequestBody String subtitleContent) {
        try {
            String jsonResponse = videoService.extractConcepts(subtitleContent);
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("提取知识点失败", e);
            return VideoResponse.error("提取知识点失败: " + e.getMessage());
        }
    }

    /**
     * 提取金句
     */
    @PostMapping("/quotes")
    public VideoResponse extractQuotes(@RequestBody String subtitleContent) {
        try {
            String jsonResponse = videoService.extractQuotes(subtitleContent);
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("提取金句失败", e);
            return VideoResponse.error("提取金句失败: " + e.getMessage());
        }
    }

    /**
     * 关键词搜索
     */
    @PostMapping("/search")
    public VideoResponse searchKeyword(@RequestBody SearchRequest request) {
        try {
            String jsonResponse = videoService.searchKeyword(
                request.getSubtitleContent(), request.getKeyword());
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return VideoResponse.error("搜索失败: " + e.getMessage());
        }
    }
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat(api): add extract, quotes and search endpoints"
```

---

### Task 1.7: 添加智能问答和流式 API 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 添加 ask 和 stream/ask 端点**

```java
    /**
     * 智能问答（自动意图分类）
     */
    @PostMapping("/ask")
    public SmartAskResponse smartAsk(
            @RequestBody ChatRequest request,
            @RequestParam(value = "debug", required = false, defaultValue = "false") Boolean debug) {
        try {
            String answer = videoService.smartAsk(
                request.getSubtitleContent(), request.getQuestion());

            if (Boolean.TRUE.equals(debug)) {
                IntentResult intentResult = intentClassificationService
                    .classifyIntentWithCache(request.getQuestion());
                return new SmartAskResponse(
                    intentResult.getIntent().name(),
                    intentResult.getConfidence(),
                    answer);
            }
            return new SmartAskResponse(answer);
        } catch (Exception e) {
            log.error("智能问答失败", e);
            SmartAskResponse response = new SmartAskResponse();
            response.setContent("智能问答失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 流式智能问答（SSE）
     */
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter smartAskStream(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question) {

        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout");
            emitter.complete();
        });

        emitter.onError(e -> log.error("SSE error", e));

        videoService.smartAskStream(subtitleContent, question)
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnComplete(emitter::complete)
            .doOnError(error -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成失败: " + error.getMessage()));
                    emitter.complete();
                } catch (IOException ignored) {}
            })
            .subscribe();

        return emitter;
    }
```

**Step 2: 验证编译通过**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat(api): add smart ask and streaming endpoints"
```

---

### Task 1.8: 创建 VideoApiController 测试

**Files:**
- Create: `src/test/java/com/example/videoagent/controller/VideoApiControllerTest.java`

**Step 1: 创建测试类**

```java
package com.example.videoagent.controller;

import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.SmartAskResponse;
import com.example.videoagent.dto.VideoResponse;
import com.example.videoagent.enums.UserIntent;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoApiController.class)
class VideoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    @Test
    void upload_withFile_shouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.srt", "text/plain", "test content".getBytes());

        mockMvc.perform(multipart("/api/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.fileName").value("test.srt"));
    }

    @Test
    void upload_content_shouldReturnSample() throws Exception {
        mockMvc.perform(post("/api/upload/content")
                .contentType(MediaType.TEXT_PLAIN)
                .content(""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void summarize_shouldReturnSummary() throws Exception {
        when(videoService.summarize(anyString())).thenReturn("Test summary");

        mockMvc.perform(post("/api/summarize")
                .contentType(MediaType.TEXT_PLAIN)
                .content("subtitle content"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.content").value("Test summary"));
    }

    @Test
    void ask_shouldReturnAnswer() throws Exception {
        when(videoService.smartAsk(anyString(), anyString())).thenReturn("Test answer");
        when(intentClassificationService.classifyIntentWithCache(anyString()))
            .thenReturn(new IntentResult(UserIntent.SUMMARY, 0.9));

        String requestBody = objectMapper.writeValueAsString(
            new com.example.videoagent.dto.ChatRequest("subtitle", "question"));

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Test answer"));
    }

    @Test
    void ask_withDebug_shouldReturnIntentInfo() throws Exception {
        when(videoService.smartAsk(anyString(), anyString())).thenReturn("Test answer");
        when(intentClassificationService.classifyIntentWithCache(anyString()))
            .thenReturn(new IntentResult(UserIntent.CONCEPT_EXTRACT, 0.85));

        String requestBody = objectMapper.writeValueAsString(
            new com.example.videoagent.dto.ChatRequest("subtitle", "提取知识点"));

        mockMvc.perform(post("/api/ask?debug=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Test answer"))
            .andExpect(jsonPath("$.intent").value("CONCEPT_EXTRACT"))
            .andExpect(jsonPath("$.confidence").value(0.85));
    }
}
```

**Step 2: 运行测试验证**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn test -Dtest=VideoApiControllerTest -q`
Expected: Tests run: 5, Failures: 0

**Step 3: 提交**

```bash
git add src/test/java/com/example/videoagent/controller/VideoApiControllerTest.java
git commit -m "test(api): add VideoApiController tests"
```

---

### Task 1.9: 验证后端 API 端点

**Files:**
- Test: 启动后端验证 API

**Step 1: 启动后端服务**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run &`
Wait: 等待服务启动（约 30 秒）

**Step 2: 测试 upload/content 端点**

Run:
```bash
curl -X POST http://localhost:8080/api/upload/content \
  -H "Content-Type: text/plain" \
  -d ""
```
Expected: JSON 响应包含示例字幕内容

**Step 3: 测试 ask 端点**

Run:
```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"subtitleContent":"test","question":"这个视频讲了什么"}'
```
Expected: JSON 响应包含 content 字段

**Step 4: 停止后端服务**

Run: `pkill -f "spring-boot:run"`

---

## Phase 2: 前端 Vue 开发

### Task 2.1: 创建 Vue 项目

**Files:**
- Create: `frontend/` 目录及初始文件

**Step 1: 使用 Vite 创建项目**

Run:
```bash
cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent
npm create vite@latest frontend -- --template vue-ts
```

**Step 2: 安装依赖**

Run:
```bash
cd frontend
npm install
npm install element-plus
npm install marked highlight.js
npm install -D @types/marked
```

**Step 3: 验证项目可启动**

Run: `cd frontend && npm run build`
Expected: Build successful

**Step 4: 提交**

```bash
cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent
git add frontend/
git commit -m "feat: initialize Vue 3 + TypeScript project with Vite"
```

---

### Task 2.2: 配置 Vite

**Files:**
- Modify: `frontend/vite.config.ts`

**Step 1: 更新 vite.config.ts**

```typescript
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

**Step 2: 验证配置**

Run: `cd frontend && npm run build`
Expected: Build successful

**Step 3: 提交**

```bash
git add frontend/vite.config.ts
git commit -m "chore: configure Vite proxy for API"
```

---

### Task 2.3: 创建 TypeScript 类型定义

**Files:**
- Create: `frontend/src/types/index.ts`

**Step 1: 创建类型文件**

```typescript
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
```

**Step 2: 提交**

```bash
git add frontend/src/types/index.ts
git commit -m "feat: add TypeScript type definitions"
```

---

### Task 2.4: 创建 API 调用层

**Files:**
- Create: `frontend/src/api/videoApi.ts`

**Step 1: 创建 API 封装**

```typescript
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

**Step 2: 验证编译**

Run: `cd frontend && npm run build`
Expected: Build successful

**Step 3: 提交**

```bash
git add frontend/src/api/videoApi.ts
git commit -m "feat: add API layer for backend communication"
```

---

### Task 2.5: 配置 Element Plus 和全局样式

**Files:**
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/style.css`

**Step 1: 更新 main.ts**

```typescript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import './style.css'

const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
```

**Step 2: 更新 style.css**

```css
:root {
  --primary-color: #409eff;
  --bg-color: #f5f7fa;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: var(--bg-color);
  min-height: 100vh;
}

#app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}
```

**Step 3: 验证编译**

Run: `cd frontend && npm run build`
Expected: Build successful

**Step 4: 提交**

```bash
git add frontend/src/main.ts frontend/src/style.css
git commit -m "chore: configure Element Plus and global styles"
```

---

### Task 2.6: 创建 MarkdownRenderer 组件

**Files:**
- Create: `frontend/src/components/MarkdownRenderer.vue`

**Step 1: 创建组件**

```vue
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

marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const renderedContent = computed(() => {
  return marked.parse(props.content)
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
```

**Step 2: 提交**

```bash
git add frontend/src/components/MarkdownRenderer.vue
git commit -m "feat: add MarkdownRenderer component with syntax highlighting"
```

---

### Task 2.7: 创建 FileUpload 组件

**Files:**
- Create: `frontend/src/components/FileUpload.vue`

**Step 1: 创建组件**

```vue
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
```

**Step 2: 提交**

```bash
git add frontend/src/components/FileUpload.vue
git commit -m "feat: add FileUpload component with drag-drop support"
```

---

### Task 2.8: 创建 QuickActions 组件

**Files:**
- Create: `frontend/src/components/QuickActions.vue`

**Step 1: 创建组件**

```vue
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
import type { Concept } from '../types'

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
```

**Step 2: 提交**

```bash
git add frontend/src/components/QuickActions.vue
git commit -m "feat: add QuickActions component"
```

---

### Task 2.9: 创建 ChatPanel 组件

**Files:**
- Create: `frontend/src/components/ChatPanel.vue`

**Step 1: 创建组件**

```vue
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
          <MarkdownRenderer v-if="msg.role === 'assistant'" :content="msg.content" />
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
        <el-button type="primary" @click="sendQuestion" :loading="streaming">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { videoApi } from '../api/videoApi'
import MarkdownRenderer from './MarkdownRenderer.vue'

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

let stopStream: (() => void) | null = null

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

  stopStream = videoApi.streamAsk(
    props.subtitleContent,
    q,
    (chunk) => {
      messages.value[msgIndex].content += chunk
      scrollToBottom()
    },
    (error) => {
      messages.value[msgIndex].content = '错误: ' + error
      messages.value[msgIndex].streaming = false
      streaming.value = false
    },
    () => {
      messages.value[msgIndex].streaming = false
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
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/components/ChatPanel.vue
git commit -m "feat: add ChatPanel component with streaming support"
```

---

### Task 2.10: 创建 ConceptTimeline 组件

**Files:**
- Create: `frontend/src/components/ConceptTimeline.vue`

**Step 1: 创建组件**

```vue
<template>
  <div class="card">
    <div class="card-header">知识概念时间轴</div>

    <el-timeline>
      <el-timeline-item
        v-for="(concept, index) in concepts"
        :key="index"
        :timestamp="concept.timestampFrom"
        placement="top"
      >
        <el-card>
          <h4>{{ concept.concept }}</h4>
          <p>{{ concept.description }}</p>
          <div class="time-range">
            <el-tag size="small" type="info">
              {{ concept.timestampFrom }} - {{ concept.timestampTo }}
            </el-tag>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import type { Concept } from '../types'

defineProps<{
  concepts: Concept[]
}>()
</script>

<style scoped>
.el-card h4 {
  margin: 0 0 8px;
  color: #303133;
}

.el-card p {
  margin: 0 0 8px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.time-range {
  margin-top: 8px;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/components/ConceptTimeline.vue
git commit -m "feat: add ConceptTimeline component"
```

---

### Task 2.11: 创建 App.vue 主组件

**Files:**
- Modify: `frontend/src/App.vue`

**Step 1: 更新 App.vue**

```vue
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
```

**Step 2: 验证编译**

Run: `cd frontend && npm run build`
Expected: Build successful

**Step 3: 提交**

```bash
git add frontend/src/App.vue
git commit -m "feat: complete App.vue with all components"
```

---

### Task 2.12: 前后端联调测试

**Files:**
- Test: 前后端联调

**Step 1: 启动后端**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run &`
Wait: 等待服务启动

**Step 2: 启动前端**

Run: `cd frontend && npm run dev &`
Wait: 等待 Vite 服务启动

**Step 3: 浏览器测试**

Open: `http://localhost:5173`
Test:
1. 点击"使用示例字幕"
2. 点击"生成摘要"
3. 在问答框输入问题并发送

Expected: 所有功能正常工作

**Step 4: 停止服务**

Run: `pkill -f "spring-boot:run"; pkill -f "vite"`

---

## Phase 3: 整合部署

### Task 3.1: 配置 Maven 前端构建插件

**Files:**
- Modify: `pom.xml`

**Step 1: 添加 frontend-maven-plugin**

在 `<build><plugins>` 中添加：

```xml
<!-- 前端构建插件 -->
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
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
            <configuration>
                <nodeVersion>v20.10.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>npm run build</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <phase>prepare-package</phase>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- 复制前端构建产物到 static -->
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend-build</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
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

**Step 2: 验证 pom.xml 语法**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn validate`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add pom.xml
git commit -m "build: add frontend-maven-plugin for integrated build"
```

---

### Task 3.2: 配置 SPA 路由回退

**Files:**
- Create: `src/main/java/com/example/videoagent/config/SpaConfig.java`

**Step 1: 创建 SPA 配置**

```java
package com.example.videoagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA 路由配置
 * 将所有非 API、非静态资源请求转发到 index.html
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add src/main/java/com/example/videoagent/config/SpaConfig.java
git commit -m "feat: add SPA routing configuration"
```

---

### Task 3.3: 完整构建验证

**Files:**
- Test: 完整构建流程

**Step 1: 完整构建**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn clean package -DskipTests`
Wait: 约 3-5 分钟（首次会下载 Node.js）
Expected: BUILD SUCCESS

**Step 2: 验证 JAR 内容**

Run: `jar tf target/video-agent-0.0.1-SNAPSHOT.jar | grep "static/" | head -10`
Expected: 看到 `static/index.html`, `static/assets/` 等

**Step 3: 运行 JAR**

Run: `java -jar target/video-agent-0.0.1-SNAPSHOT.jar &`
Wait: 等待服务启动

**Step 4: 浏览器测试**

Open: `http://localhost:8080`
Expected: Vue SPA 正常加载

**Step 5: 测试 API**

Run: `curl http://localhost:8080/api/upload/content -X POST -H "Content-Type: text/plain" -d ""`
Expected: JSON 响应

**Step 6: 停止服务**

Run: `pkill -f "video-agent"`

**Step 7: 最终提交**

```bash
git add .
git commit -m "feat: complete Vue frontend refactor with single JAR deployment"
```

---

## 完成检查清单

- [ ] 后端 ChatRequest 包含 subtitleContent
- [ ] 后端 SearchRequest 已创建
- [ ] VideoApiController 所有端点正常
- [ ] VideoApiControllerTest 测试通过
- [ ] 前端 Vue 项目创建成功
- [ ] TypeScript 类型定义完整
- [ ] API 调用层正常工作
- [ ] FileUpload 组件正常
- [ ] QuickActions 组件正常
- [ ] ChatPanel 组件正常（含流式）
- [ ] ConceptTimeline 组件正常
- [ ] MarkdownRenderer 组件正常
- [ ] Maven 完整构建成功
- [ ] 单 JAR 部署正常
- [ ] SPA 路由正常
