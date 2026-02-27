# Deep Reasoning (CoT) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 CoT 深度推理模式，用户通过 `/deep` 或 `深度分析：` 前缀触发

**Architecture:** 前缀检测 → DEEP_QA 意图 → 专用 Prompt 模板 → 自然语言推理输出

**Tech Stack:** Java 21, Spring AI Alibaba, Text Blocks

---

## Task 1: 扩展 UserIntent 枚举

**Files:**
- Modify: `src/main/java/com/example/videoagent/enums/UserIntent.java`

**Step 1: 添加 DEEP_QA 枚举值**

修改文件，添加新的枚举值：

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
    SEARCH_KEYWORD,   // 关键词搜索
    DEEP_QA           // 深度分析（CoT推理）
}
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 2: 添加 DEEP_QA_PROMPT_TEMPLATE

**Files:**
- Modify: `src/main/java/com/example/videoagent/config/PromptConstants.java`

**Step 1: 在 PromptConstants.java 末尾添加新的 Prompt 模板**

在 `SEARCH_KEYWORD_PROMPT_TEMPLATE` 之后添加：

```java
    /**
     * 深度分析 Prompt 模板
     * 设计要点：CoT (思维链) + 自然语言推理过程
     */
    public static final String DEEP_QA_PROMPT_TEMPLATE = """
        【深度分析示例】
        字幕内容：...[00:05:00] Transformer 的自注意力机制可以并行计算...
                   [00:08:30] 但它的计算复杂度是 O(n²)，对长序列不友好...
                   [00:12:00] 相比 RNN，Transformer 训练更快但显存占用更高...
        用户问题：Transformer 有什么优缺点？

        助手回答：
        让我仔细分析一下视频中关于 Transformer 的讨论。

        首先，关于优势方面，视频在 [00:05:00] 提到自注意力机制支持并行计算，这意味着训练效率大幅提升。这是 Transformer 相比 RNN 的核心优势。

        但是，视频也在 [00:08:30] 指出了一个重要问题：计算复杂度是 O(n²)。这意味着当序列变长时，计算成本会急剧增加，对长文本处理不太友好。

        另外，[00:12:00] 还提到了一个权衡：虽然训练更快，但显存占用也更高。这在实际部署时需要特别注意。

        综合来看，Transformer 的并行能力是其最大优势，但长序列处理和显存消耗是需要权衡的因素。

        ========================================
        以下是实际任务：
        ========================================

        %s

        ---
        用户问题：%s

        请基于以上视频字幕进行深度分析。在回答之前，请先逐步思考视频中提到的相关论据，理清它们之间的逻辑关系，最后再给出你的结论。回答要自然流畅，像在和朋友讨论一样。
        """;
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 3: 更新 INTENT_CLASSIFICATION_PROMPT

**Files:**
- Modify: `src/main/java/com/example/videoagent/config/PromptConstants.java`

**Step 1: 更新意图分类 Prompt**

在 `INTENT_CLASSIFICATION_PROMPT` 的【意图类型】部分添加 DEEP_QA 说明：

找到 `INTENT_CLASSIFICATION_PROMPT` 常量，在 `- SEARCH_KEYWORD:` 之后添加：

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

        - DEEP_QA: 用户请求深度分析，希望获得详细的推理过程
          示例: "分析一下..."、"详细解释..."、"深入讨论..."

        【输出格式】
        只输出一个 JSON 对象，不要有任何其他内容：
        {"intent": "意图类型", "confidence": 0.0-1.0}

        【用户问题】
        %s
        """;
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 4: 添加前缀检测逻辑

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/IntentClassificationService.java`

**Step 1: 在 classifyIntentWithCache 方法开头添加前缀检测**

修改 `classifyIntentWithCache` 方法：

```java
    /**
     * 带缓存的意图分类
     */
    public IntentResult classifyIntentWithCache(String question) {
        // 0. 前缀检测：深度分析模式
        if (question.startsWith("/deep ") || question.startsWith("深度分析：") || question.startsWith("深度分析:")) {
            return new IntentResult(UserIntent.DEEP_QA, 1.0);
        }

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
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 5: 添加 deepAnalyze 接口方法

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoService.java`

**Step 1: 在接口中添加 deepAnalyze 方法**

在 `smartAsk` 方法之后添加：

```java
    /**
     * 深度分析问答
     * 启用 CoT (思维链) 模式进行推理
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return 深度分析回答
     */
    String deepAnalyze(String subtitleContent, String question);
```

**Step 2: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS (会报错 VideoServiceImpl 未实现，正常)

---

## Task 6: 实现 deepAnalyze 方法

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoServiceImpl.java`

**Step 1: 添加 deepAnalyze 实现**

在 `VideoServiceImpl` 类中添加方法：

```java
    @Override
    public String deepAnalyze(String subtitleContent, String question) {
        // 移除前缀，获取真实问题
        String realQuestion = question;
        if (question.startsWith("/deep ")) {
            realQuestion = question.substring(6).trim();
        } else if (question.startsWith("深度分析：") || question.startsWith("深度分析:")) {
            realQuestion = question.substring(5).trim();
        }

        String userPrompt = String.format(
                PromptConstants.DEEP_QA_PROMPT_TEMPLATE,
                subtitleContent,
                realQuestion
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }
```

**Step 2: 在 smartAsk 方法中添加 DEEP_QA 分支**

修改 `smartAsk` 方法的 switch 语句：

```java
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
            case DEEP_QA -> deepAnalyze(subtitleContent, question);
        };
    }
```

**Step 3: 验证编译**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 7: 验证并提交

**Step 1: 完整编译测试**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn clean compile -q`
Expected: BUILD SUCCESS

**Step 2: 启动应用验证**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run -q`
Expected: 应用启动成功

**Step 3: API 测试**

测试深度分析模式：

```bash
curl -X POST http://localhost:8080/api/video/smart-ask \
  -H "Content-Type: application/json" \
  -d '{
    "subtitleContent": "[00:05:00] Transformer 的自注意力机制可以并行计算\n[00:08:30] 但它的计算复杂度是 O(n²)\n[00:12:00] 相比 RNN，Transformer 训练更快但显存占用更高",
    "question": "/deep 分析Transformer的优缺点"
  }'
```

Expected: 返回包含自然语言推理过程的深度分析

测试普通问答（不应触发深度模式）：

```bash
curl -X POST http://localhost:8080/api/video/smart-ask \
  -H "Content-Type: application/json" \
  -d '{
    "subtitleContent": "[00:05:00] Transformer 的自注意力机制",
    "question": "什么是Transformer"
  }'
```

Expected: 返回普通问答结果

**Step 4: 提交代码**

```bash
git add src/main/java/com/example/videoagent/enums/UserIntent.java \
        src/main/java/com/example/videoagent/config/PromptConstants.java \
        src/main/java/com/example/videoagent/service/VideoService.java \
        src/main/java/com/example/videoagent/service/VideoServiceImpl.java \
        src/main/java/com/example/videoagent/service/IntentClassificationService.java

git commit -m "feat: add deep reasoning (CoT) mode

- Add DEEP_QA intent type to UserIntent enum
- Add DEEP_QA_PROMPT_TEMPLATE with CoT guidance
- Add prefix detection for /deep and 深度分析：
- Implement deepAnalyze method in VideoService
- Route DEEP_QA in smartAsk switch

Trigger: /deep <question> or 深度分析：<question>

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收清单

- [ ] `UserIntent` 包含 `DEEP_QA` 枚举值
- [ ] `DEEP_QA_PROMPT_TEMPLATE` 包含 CoT 引导指令
- [ ] 前缀检测正确识别 `/deep` 和 `深度分析：`
- [ ] `deepAnalyze` 方法正确移除前缀并调用 Prompt
- [ ] `smartAsk` 正确路由到 `deepAnalyze` 方法
- [ ] API 测试返回自然语言的深度分析结果
