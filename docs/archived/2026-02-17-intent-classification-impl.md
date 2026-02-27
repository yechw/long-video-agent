# Intent Classification 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 Intent Classification 功能，让 Agent 能自动识别用户意图并路由到专用 Prompt。

**Architecture:** 采用 Prompt Chaining 架构，先通过 LLM 分类意图，再路由到对应的专用 Prompt 执行。新增 `/ask` 智能入口，同时保留现有三个端点。

**Tech Stack:** Spring Boot 3.2.5, Spring AI Alibaba 1.0.0-M6.1, Caffeine Cache, Thymeleaf

---

## Task 1: 添加 Caffeine 依赖

**Files:**
- Modify: `pom.xml:63-69`

**Step 1: 添加 Caffeine 依赖**

在 `</dependencies>` 标签前添加：

```xml
        <!-- Caffeine Cache -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
```

**Step 2: 验证依赖下载**

Run: `cd LongVideoAgent && mvn dependency:resolve -DincludeScope=compile | grep caffeine`
Expected: 输出包含 `caffeine`

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add caffeine cache dependency"
```

---

## Task 2: 创建 UserIntent 枚举

**Files:**
- Create: `src/main/java/com/example/videoagent/enums/UserIntent.java`

**Step 1: 创建枚举类**

```java
package com.example.videoagent.enums;

/**
 * 用户意图类型枚举
 * 用于意图分类
 */
public enum UserIntent {
    SUMMARIZE,        // 总结
    QA,               // 问答
    EXTRACT_CONCEPTS, // 提取知识点
    EXTRACT_QUOTES,   // 金句提取
    SEARCH_KEYWORD    // 关键词搜索
}
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/enums/UserIntent.java
git commit -m "feat: add UserIntent enum for intent classification"
```

---

## Task 3: 创建 IntentResult DTO

**Files:**
- Create: `src/main/java/com/example/videoagent/dto/IntentResult.java`

**Step 1: 创建 DTO 类**

```java
package com.example.videoagent.dto;

import com.example.videoagent.enums.UserIntent;

/**
 * 意图分类结果
 */
public class IntentResult {

    private UserIntent intent;
    private Double confidence;

    public IntentResult() {}

    public IntentResult(UserIntent intent, Double confidence) {
        this.intent = intent;
        this.confidence = confidence;
    }

    public UserIntent getIntent() {
        return intent;
    }

    public void setIntent(UserIntent intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/dto/IntentResult.java
git commit -m "feat: add IntentResult DTO for classification results"
```

---

## Task 4: 创建 SmartAskResponse DTO

**Files:**
- Create: `src/main/java/com/example/videoagent/dto/SmartAskResponse.java`

**Step 1: 创建响应 DTO**

```java
package com.example.videoagent.dto;

/**
 * 智能问答响应
 */
public class SmartAskResponse {

    private String intent;      // 调试模式时返回
    private Double confidence;  // 调试模式时返回
    private String content;     // 最终结果

    public SmartAskResponse() {}

    public SmartAskResponse(String content) {
        this.content = content;
    }

    public SmartAskResponse(String intent, Double confidence, String content) {
        this.intent = intent;
        this.confidence = confidence;
        this.content = content;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/dto/SmartAskResponse.java
git commit -m "feat: add SmartAskResponse DTO for /ask endpoint"
```

---

## Task 5: 添加新 Prompt 模板

**Files:**
- Modify: `src/main/java/com/example/videoagent/config/PromptConstants.java`

**Step 1: 在文件末尾 `}` 前添加三个新 Prompt 模板**

```java
    /**
     * 意图分类 Prompt
     * 用于识别用户意图类型
     */
    public static final String INTENT_CLASSIFICATION_PROMPT = """
        你是一个意图分类器。分析用户问题，判断用户想要执行什么操作。

        【意图类型】
        - SUMMARIZE: 用户想要视频的总结或概览
          示例: "总结一下"、"这个视频讲了什么"、"给我一个概览"

        - QA: 用户有具体问题需要回答
          示例: "什么是RAG？"、"Transformer有什么优势？"

        - EXTRACT_CONCEPTS: 用户想要提取知识点或核心概念
          示例: "提取知识点"、"有哪些核心概念"、"列出关键点"

        - EXTRACT_QUOTES: 用户想要提取金句或精彩语录
          示例: "有哪些金句"、"精彩语录"、"给我一些好句子"

        - SEARCH_KEYWORD: 用户想要搜索关键词在视频中的位置
          示例: "哪里提到了..."、"在什么位置说了..."、"搜索..."

        【输出格式】
        只输出一个 JSON 对象，不要有任何其他内容：
        {"intent": "意图类型", "confidence": 0.0-1.0}

        【用户问题】
        %s
        """;

    /**
     * 金句提取 Prompt 模板
     */
    public static final String EXTRACT_QUOTES_PROMPT_TEMPLATE = """
        【示例】
        字幕内容：
        [00:05:20] AI 不会取代你，但会用 AI 的人会取代你
        [00:12:45] 最好的代码是没有代码，最好的提示是没有提示

        助手输出：
        [
          {
            "timestamp": "00:05:20",
            "quote": "AI 不会取代你，但会用 AI 的人会取代你",
            "context": "讨论 AI 时代个人竞争力的变化"
          },
          {
            "timestamp": "00:12:45",
            "quote": "最好的代码是没有代码，最好的提示是没有提示",
            "context": "强调简化思维的重要性"
          }
        ]

        ========================================
        以下是实际任务：
        ========================================

        %s

        ---
        基于以上视频字幕内容，提取 5-10 条金句或精彩语录。

        【输出要求】
        1. 必须输出纯 JSON 数组格式
        2. 时间戳格式统一为 HH:MM:SS
        3. 如果无明确金句，输出空数组 []
        4. 不要包含任何开场白或结束语

        【字段说明】
        - timestamp: 金句出现的时间戳
        - quote: 金句原文
        - context: 金句的上下文说明
        """;

    /**
     * 关键词搜索 Prompt 模板
     */
    public static final String SEARCH_KEYWORD_PROMPT_TEMPLATE = """
        【示例】
        字幕内容：
        [00:01:00] 今天我们来聊聊 Transformer 架构
        [00:05:30] Transformer 的核心是自注意力机制
        [00:10:00] 相比 RNN，Transformer 可以并行计算
        [00:15:20] BERT 和 GPT 都基于 Transformer

        搜索关键词：Transformer

        助手输出：
        {
          "keyword": "Transformer",
          "occurrences": [
            {
              "timestamp": "00:01:00",
              "context": "今天我们来聊聊 Transformer 架构"
            },
            {
              "timestamp": "00:05:30",
              "context": "Transformer 的核心是自注意力机制"
            },
            {
              "timestamp": "00:10:00",
              "context": "相比 RNN，Transformer 可以并行计算"
            },
            {
              "timestamp": "00:15:20",
              "context": "BERT 和 GPT 都基于 Transformer"
            }
          ],
          "summary": "视频中 4 处提到 Transformer，主要讨论其架构原理和与 RNN 的对比"
        }

        ========================================
        以下是实际任务：
        ========================================

        %s

        ---
        搜索关键词：%s

        请找出该关键词在视频中的所有出现位置。
        """;
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/config/PromptConstants.java
git commit -m "feat: add intent classification and new prompt templates"
```

---

## Task 6: 创建 IntentClassificationService

**Files:**
- Create: `src/main/java/com/example/videoagent/service/IntentClassificationService.java`

**Step 1: 创建意图分类服务**

```java
package com.example.videoagent.service;

import com.example.videoagent.config.PromptConstants;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 意图分类服务
 * 使用 LLM 进行意图识别，带缓存
 */
@Service
public class IntentClassificationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // Caffeine 缓存：问题 -> 意图结果
    private final Cache<String, IntentResult> intentCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    // 置信度阈值，低于此值默认走 QA
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    public IntentClassificationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 带缓存的意图分类
     */
    public IntentResult classifyIntentWithCache(String question) {
        // 1. 尝试从缓存获取
        IntentResult cached = intentCache.getIfPresent(question);
        if (cached != null) {
            return cached;
        }

        // 2. 调用 LLM 分类
        IntentResult result = classifyIntent(question);

        // 3. 存入缓存
        intentCache.put(question, result);

        return result;
    }

    /**
     * 调用 LLM 进行意图分类
     */
    public IntentResult classifyIntent(String question) {
        String prompt = String.format(PromptConstants.INTENT_CLASSIFICATION_PROMPT, question);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseIntentResult(response);
    }

    /**
     * 解析 LLM 返回的意图结果
     */
    private IntentResult parseIntentResult(String response) {
        try {
            // 提取 JSON 对象
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);

                // 解析 JSON
                var node = objectMapper.readTree(json);
                String intentStr = node.get("intent").asText();
                double confidence = node.get("confidence").asDouble();

                UserIntent intent = UserIntent.valueOf(intentStr);

                // 置信度低于阈值，默认走 QA
                if (confidence < CONFIDENCE_THRESHOLD) {
                    return new IntentResult(UserIntent.QA, confidence);
                }

                return new IntentResult(intent, confidence);
            }
        } catch (Exception e) {
            // 解析失败，默认走 QA
        }

        return new IntentResult(UserIntent.QA, 0.5);
    }
}
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/service/IntentClassificationService.java
git commit -m "feat: add IntentClassificationService with caffeine cache"
```

---

## Task 7: 扩展 VideoService 接口

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoService.java`

**Step 1: 在接口末尾 `}` 前添加新方法**

```java
    /**
     * 提取金句
     * @param subtitleContent 字幕内容
     * @return JSON 格式的金句列表
     */
    String extractQuotes(String subtitleContent);

    /**
     * 搜索关键词
     * @param subtitleContent 字幕内容
     * @param keyword 关键词
     * @return JSON 格式的搜索结果
     */
    String searchKeyword(String subtitleContent, String keyword);

    /**
     * 智能问答（自动意图分类 + 路由执行）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return 回答内容
     */
    String smartAsk(String subtitleContent, String question);
```

**Step 2: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS (会有未实现方法的警告)

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/service/VideoService.java
git commit -m "feat: add new methods to VideoService interface"
```

---

## Task 8: 实现 VideoServiceImpl 新方法

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoServiceImpl.java`

**Step 1: 添加依赖注入和导入**

在文件顶部添加导入：

```java
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
```

修改构造函数，注入 `IntentClassificationService`：

```java
@Service
public class VideoServiceImpl implements VideoService {

    private final ChatClient chatClient;
    private final IntentClassificationService intentClassificationService;

    public VideoServiceImpl(ChatClient.Builder chatClientBuilder,
                           IntentClassificationService intentClassificationService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(PromptConstants.SYSTEM_PROMPT)
                .build();
        this.intentClassificationService = intentClassificationService;
    }
```

**Step 2: 在类末尾 `}` 前添加新方法实现**

```java
    @Override
    public String extractQuotes(String subtitleContent) {
        String userPrompt = String.format(
                PromptConstants.EXTRACT_QUOTES_PROMPT_TEMPLATE,
                subtitleContent
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String searchKeyword(String subtitleContent, String keyword) {
        String userPrompt = String.format(
                PromptConstants.SEARCH_KEYWORD_PROMPT_TEMPLATE,
                subtitleContent,
                keyword
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String smartAsk(String subtitleContent, String question) {
        // Step 1: 意图分类
        IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
        UserIntent intent = intentResult.getIntent();

        // Step 2: 根据意图路由到对应的专用 Prompt
        return switch (intent) {
            case SUMMARIZE -> summarize(subtitleContent);
            case QA -> chat(subtitleContent, question);
            case EXTRACT_CONCEPTS -> extractConcepts(subtitleContent);
            case EXTRACT_QUOTES -> extractQuotes(subtitleContent);
            case SEARCH_KEYWORD -> {
                // 从问题中提取关键词
                String keyword = extractKeywordFromQuestion(question);
                yield searchKeyword(subtitleContent, keyword);
            }
        };
    }

    /**
     * 从问题中提取搜索关键词
     * 简单实现：移除常见前缀词
     */
    private String extractKeywordFromQuestion(String question) {
        // 移除常见的前缀
        String keyword = question
                .replace("哪里提到了", "")
                .replace("在什么位置说了", "")
                .replace("搜索", "")
                .replace("查找", "")
                .replace("找到", "")
                .trim();

        // 如果关键词为空，返回原问题
        return keyword.isEmpty() ? question : keyword;
    }
```

**Step 3: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/com/example/videoagent/service/VideoServiceImpl.java
git commit -m "feat: implement smartAsk with intent routing in VideoServiceImpl"
```

---

## Task 9: 添加 /ask 端点到 Controller

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoController.java`

**Step 1: 添加导入和新的端点方法**

在文件顶部添加导入：

```java
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.SmartAskResponse;
import com.example.videoagent.service.IntentClassificationService;
```

修改构造函数，注入 `IntentClassificationService`：

```java
@Controller
@RequestMapping("/")
public class VideoController {

    private final VideoService videoService;
    private final IntentClassificationService intentClassificationService;

    public VideoController(VideoService videoService,
                          IntentClassificationService intentClassificationService) {
        this.videoService = videoService;
        this.intentClassificationService = intentClassificationService;
    }
```

**Step 2: 在类末尾 `}` 前添加新的端点**

```java
    /**
     * 智能问答入口
     * 自动识别意图并路由到专用 Prompt
     */
    @PostMapping("/ask")
    public String smartAsk(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question,
            @RequestParam(value = "debug", required = false, defaultValue = "false") Boolean debug,
            Model model) {

        try {
            // 执行智能问答
            String answer = videoService.smartAsk(subtitleContent, question);

            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
            model.addAttribute("smartQuestion", question);
            model.addAttribute("smartAnswer", answer);

            // 调试模式：返回意图信息
            if (Boolean.TRUE.equals(debug)) {
                IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
                model.addAttribute("debugIntent", intentResult.getIntent().name());
                model.addAttribute("debugConfidence", intentResult.getConfidence());
            }
        } catch (Exception e) {
            model.addAttribute("error", "智能问答失败: " + e.getMessage());
            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
        }

        return "index";
    }
```

**Step 3: 验证编译**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/com/example/videoagent/controller/VideoController.java
git commit -m "feat: add /ask endpoint with optional debug mode"
```

---

## Task 10: 更新前端 UI

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: 在快捷操作区域后添加智能问答入口**

在 `<!-- 操作区域 -->` section 的 `</section>` 后，`<!-- 知识点时间线 -->` section 前添加：

```html
        <!-- 智能问答区域 -->
        <section class="smart-ask-section" th:if="${subtitleLoaded}">
            <h2>🤖 智能问答 (自动识别意图)</h2>
            <p class="hint">输入任意问题，AI 会自动判断你的意图（总结/问答/知识点/金句/搜索）</p>
            <form action="/ask" method="post">
                <input type="hidden" name="subtitleContent" th:value="${subtitleContent}">
                <div class="chat-input-wrapper">
                    <textarea name="question" placeholder="例如：总结一下这个视频 / 有哪些金句 / 哪里提到了 Transformer" rows="3"
                              th:text="${smartQuestion}"></textarea>
                </div>
                <div class="option-group">
                    <label class="checkbox-label">
                        <input type="checkbox" name="debug" value="true">
                        显示调试信息（意图分类结果）
                    </label>
                </div>
                <button type="submit" class="btn btn-primary">智能回答</button>
            </form>
        </section>

        <!-- 智能问答结果 -->
        <section class="result-section" th:if="${smartAnswer}">
            <h2>🎯 智能回答</h2>
            <div class="result-card">
                <p class="question-preview" th:if="${smartQuestion}">
                    <strong>问题：</strong><span th:text="${smartQuestion}"></span>
                </p>
                <div class="debug-info" th:if="${debugIntent}">
                    <span class="debug-badge">意图: <strong th:text="${debugIntent}">-</strong></span>
                    <span class="debug-badge">置信度: <strong th:text="${#numbers.formatDecimal(debugConfidence, 1, 2)}">-</strong></span>
                </div>
                <pre th:text="${smartAnswer}"></pre>
            </div>
        </section>
```

**Step 2: 验证编译和模板解析**

Run: `cd LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/resources/templates/index.html
git commit -m "feat: add smart ask UI section with debug option"
```

---

## Task 11: 手动测试验证

**Step 1: 启动应用**

Run: `cd LongVideoAgent && mvn spring-boot:run`
Expected: 应用启动成功，监听 8080 端口

**Step 2: 测试场景**

1. **访问** `http://localhost:8080`
2. **上传示例字幕**
3. **测试智能问答**：
   - 输入 "总结一下" → 应返回总结
   - 输入 "什么是提示工程？" → 应返回问答
   - 输入 "有哪些金句" → 应返回金句列表
   - 输入 "哪里提到了 Transformer" → 应返回搜索结果
4. **测试调试模式**：
   - 勾选 "显示调试信息"
   - 输入问题，查看返回的意图类型和置信度

**Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete intent classification implementation

- Add UserIntent enum with 5 intent types
- Add IntentClassificationService with Caffeine cache
- Add /ask endpoint with optional debug mode
- Update UI with smart ask section
- Support: summarize, QA, concepts, quotes, keyword search"
```

---

## 文件变更汇总

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 添加 Caffeine 依赖 |
| `enums/UserIntent.java` | 新增 | 意图类型枚举 |
| `dto/IntentResult.java` | 新增 | 意图分类结果 |
| `dto/SmartAskResponse.java` | 新增 | 智能问答响应 |
| `config/PromptConstants.java` | 修改 | 新增 3 个 Prompt 模板 |
| `service/IntentClassificationService.java` | 新增 | 意图分类服务 |
| `service/VideoService.java` | 修改 | 新增 3 个方法 |
| `service/VideoServiceImpl.java` | 修改 | 实现新方法 |
| `controller/VideoController.java` | 修改 | 新增 /ask 端点 |
| `templates/index.html` | 修改 | 新增智能问答 UI |
