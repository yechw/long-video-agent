# Intent Classification 设计文档

## 概述

本文档描述 LongVideoAgent 的意图分类功能设计，实现 Prompt Chaining 架构：先识别用户意图，再路由到专用 Prompt 执行。

## 需求总结

| 维度 | 决策 |
|------|------|
| 架构模式 | 混合模式：保留 3 个端点 + 新增 `/ask` 智能入口 |
| 意图类型 | 5 种：总结、问答、知识点、金句、关键词 |
| 分类方式 | LLM Prompt 分类 |
| 模糊处理 | 默认路由到问答 |
| 响应格式 | 可选调试模式 `?debug=true` |

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      VideoController                         │
│  /summarize  /chat  /extract  │  /ask (新增)                │
└────────────────────────────────┼────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    IntentClassificationService (新增)        │
│  - classifyIntent(question) → UserIntent                     │
│  - classifyIntentWithCache(question) → UserIntent            │
└────────────────────────────────┼────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    VideoServiceImpl (扩展)                   │
│  - summarize()      chat()      extractConcepts()           │
│  - extractQuotes()  searchKeyword()  (新增)                  │
│  - smartAsk() (新增：整合意图分类+路由执行)                    │
└─────────────────────────────────────────────────────────────┘
```

## 核心组件

### 1. UserIntent 枚举

```java
public enum UserIntent {
    SUMMARIZE,        // 总结
    QA,               // 问答
    EXTRACT_CONCEPTS, // 提取知识点
    EXTRACT_QUOTES,   // 金句提取
    SEARCH_KEYWORD    // 关键词搜索
}
```

### 2. IntentClassificationService

职责：封装意图分类逻辑 + 缓存

```java
@Service
public class IntentClassificationService {

    private final ChatClient chatClient;

    // Caffeine 缓存配置
    private final Cache<String, IntentResult> intentCache =
        Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public IntentResult classifyIntentWithCache(String question);
    public IntentResult classifyIntent(String question);
}
```

### 3. 意图分类 Prompt

```java
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
```

## 新增专用 Prompt

### 金句提取 Prompt

```java
public static final String EXTRACT_QUOTES_PROMPT_TEMPLATE = """
    【示例】
    字幕内容：
    [00:05:20] AI 不会取代你，但会用 AI 的人会取代你

    助手输出：
    [
      {
        "timestamp": "00:05:20",
        "quote": "AI 不会取代你，但会用 AI 的人会取代你",
        "context": "讨论 AI 时代个人竞争力的变化"
      }
    ]

    ========================================
    %s
    ---
    基于以上视频字幕内容，提取 5-10 条金句或精彩语录。

    【输出要求】
    1. 必须输出纯 JSON 数组格式
    2. 时间戳格式统一为 HH:MM:SS
    3. 如果无明确金句，输出空数组 []
    """;
```

### 关键词搜索 Prompt

```java
public static final String SEARCH_KEYWORD_PROMPT_TEMPLATE = """
    【示例】
    字幕内容：
    [00:01:00] 今天我们来聊聊 Transformer 架构

    搜索关键词：Transformer

    助手输出：
    {
      "keyword": "Transformer",
      "occurrences": [
        {
          "timestamp": "00:01:00",
          "context": "今天我们来聊聊 Transformer 架构"
        }
      ],
      "summary": "视频中 1 处提到 Transformer"
    }

    ========================================
    %s
    ---
    搜索关键词：%s
    """;
```

## API 接口

### POST /ask

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| subtitleContent | String | 是 | 字幕内容 |
| question | String | 是 | 用户问题 |
| debug | Boolean | 否 | 是否返回调试信息，默认 false |

**响应格式**：

普通模式：
```json
{
  "content": "根据视频内容..."
}
```

调试模式：
```json
{
  "intent": "QA",
  "confidence": 0.95,
  "content": "根据视频内容..."
}
```

## 缓存策略

| 参数 | 值 | 理由 |
|------|-----|------|
| 最大条目 | 100 | 学习项目，避免内存占用过大 |
| 过期时间 | 10 分钟 | 平衡命中率与内存释放 |
| 缓存键 | 原始问题文本 | 简单直接 |

## 文件结构

```
新增文件:
- dto/IntentResult.java
- dto/SmartAskResponse.java
- enums/UserIntent.java
- service/IntentClassificationService.java

修改文件:
- config/PromptConstants.java
- service/VideoService.java
- service/VideoServiceImpl.java
- controller/VideoController.java
- resources/templates/index.html
```

## 依赖

需要添加 Caffeine 缓存库：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## 设计决策记录

1. **为什么选择两次 LLM 调用而非单次**：符合 "Break Complex Tasks into Simpler Subtasks" 原则，每个 Prompt 职责单一，便于独立优化。

2. **为什么模糊意图默认走 QA**：QA 是最通用的意图类型，能处理各种问题，用户体验最好。

3. **为什么使用 Caffeine 而非 Spring Cache**：Caffeine 是高性能本地缓存，无需额外配置，适合学习项目。
