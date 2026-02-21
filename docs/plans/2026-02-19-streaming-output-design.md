# 流式输出改造设计文档

> 创建日期: 2026-02-19

## 一、背景

当前项目采用同步请求模式，用户提问后需等待 AI 完整生成回答才能看到结果。
为提升用户体验，需要将前后端交互改造为流式输出，实现打字机效果。

## 二、设计方案

### 2.1 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend                              │
│  ┌─────────────┐     EventSource      ┌─────────────────┐   │
│  │ Thymeleaf   │ ──────────────────▶  │  流式响应区域    │   │
│  │  + JS       │     SSE Events       │  (实时更新)      │   │
│  └─────────────┘                      └─────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
│  /ask          → 同步接口（保留）                            │
│  /stream/ask   → 流式接口（新增，返回 SseEmitter）           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Service Layer                           │
│  smartAsk()         → String（保留）                         │
│  smartAskStream()   → Flux<String>（新增）                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Spring AI ChatClient                     │
│  .call().content()   → 同步                                  │
│  .stream().content() → Flux<String>                          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 技术选型

| 维度 | 选择 | 理由 |
|------|------|------|
| 传输协议 | SSE (Server-Sent Events) | 单向推送足够，实现简单 |
| 后端框架 | Spring MVC + SseEmitter | 与现有架构一致，无额外依赖 |
| 前端方案 | Thymeleaf + 原生 JS (EventSource) | 改动最小，适合学习项目 |
| Service 层 | 提供流式方法 | 分层清晰，便于复用测试 |

## 三、后端改动

### 3.1 Service 层

**VideoService.java** 新增接口：

```java
/**
 * 智能问答流式输出
 */
Flux<String> smartAskStream(String subtitleContent, String question);
```

**VideoServiceImpl.java** 实现：

```java
@Override
public Flux<String> smartAskStream(String subtitleContent, String question) {
    IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
    UserIntent intent = intentResult.getIntent();

    String userPrompt = buildPromptByIntent(subtitleContent, question, intent);

    return chatClient.prompt()
            .user(userPrompt)
            .stream()
            .content();
}
```

### 3.2 Controller 层

**VideoController.java** 新增：

```java
@GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter smartAskStream(
        @RequestParam("subtitleContent") String subtitleContent,
        @RequestParam("question") String question) {

    SseEmitter emitter = new SseEmitter(60_000L); // 60秒超时

    // 超时处理
    emitter.onTimeout(() -> emitter.complete());

    // 异常处理
    emitter.onError(e -> log.error("SSE error", e));

    videoService.smartAskStream(subtitleContent, question)
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

## 四、前端改动

### 4.1 页面新增流式响应区域

在 `index.html` 中新增：

```html
<!-- 流式回答区域 -->
<div id="stream-response" class="stream-container" style="display: none;">
    <div class="stream-answer" id="stream-answer"></div>
</div>
```

### 4.2 JavaScript 处理 SSE

```javascript
function askStream(question) {
    const streamAnswer = document.getElementById('stream-answer');
    streamAnswer.innerText = '';
    document.getElementById('stream-response').style.display = 'block';

    const url = `/stream/ask?subtitleContent=${encodeURIComponent(subtitleContent)}&question=${encodeURIComponent(question)}`;

    const eventSource = new EventSource(url);

    eventSource.onmessage = function(event) {
        streamAnswer.innerText += event.data;
    };

    eventSource.onerror = function(event) {
        streamAnswer.innerText += '\n[连接中断]';
        eventSource.close();
    };

    eventSource.addEventListener('error', function(event) {
        streamAnswer.innerText += '\n[错误] ' + event.data;
        eventSource.close();
    });
}
```

### 4.3 表单提交改为 JS 触发

```html
<button type="button" onclick="askStream(document.getElementById('question').value)">
    流式提问
</button>
```

## 五、测试策略

### 5.1 后端测试

| 测试类型 | 内容 |
|---------|------|
| 单元测试 | Service 层 `smartAskStream()` 返回 Flux |
| 集成测试 | Controller 返回正确 SSE 格式 |

### 5.2 前端自动化测试 (Playwright)

添加依赖：

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.40.0</version>
    <scope>test</scope>
</dependency>
```

测试用例：

```java
@Test
void testStreamAskOutputsIncrementally(Page page) {
    // 1. 加载页面并上传字幕
    page.navigate("http://localhost:8080");
    page.locator("#useSample").check();
    page.locator("#uploadForm").submit();

    // 2. 触发流式提问
    page.locator("#question").fill("什么是提示工程？");
    page.locator("#streamAskBtn").click();

    // 3. 等待流式响应区域显示
    page.waitForSelector("#stream-response[style*='block']");

    // 4. 验证内容逐步增长
    String content1 = page.locator("#stream-answer").innerText();
    Thread.sleep(500);
    String content2 = page.locator("#stream-answer").innerText();

    assertThat(content2.length()).isGreaterThan(content1.length());
    assertThat(content2).contains("提示");
}

@Test
void testStreamAskConnectionError(Page page) {
    // 模拟网络错误场景
    page.route("**/stream/ask**", route -> route.abort());

    page.navigate("http://localhost:8080");
    // ... 上传字幕 ...
    page.locator("#streamAskBtn").click();

    // 验证错误提示
    page.waitForSelector("#stream-answer:text('连接中断')");
}
```

测试覆盖场景：

| 场景 | 验证点 |
|------|--------|
| 正常流式输出 | 内容逐步增长、最终完整 |
| 网络错误 | 显示错误提示 |
| 超时场景 | 显示超时提示 |
| 并发请求 | 多个流式请求互不干扰 |

## 六、不改动的部分

- 现有同步接口 `/ask`、`/summarize`、`/chat` 等保持不变
- 意图分类逻辑复用现有 `IntentClassificationService`
- Prompt 模板复用现有 `PromptConstants`

## 七、待办需求（未来迭代）

### 7.1 前端改造为 Vue/React

将 Thymeleaf 服务端渲染改造为 Vue 或 React 单页应用：
- 组件化管理流式响应
- 更好的用户体验和交互
- 支持更复杂的前端功能

### 7.2 后端改造为 Spring WebFlux

将 Spring MVC 改造为响应式 WebFlux 架构：
- 原生响应式支持，代码更简洁
- 更好的背压控制
- 更高并发性能
- 统一的响应式编程模型
