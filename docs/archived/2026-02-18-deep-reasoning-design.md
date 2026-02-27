# 任务 7: 深度推理设计文档

> 创建日期: 2026-02-18
> 状态: 待实现
> 对应章节: AI Engineering 5.2 Give the Model Time to Think

## 1. 概述

### 1.1 目标
实现 CoT (Chain-of-Thought) 深度推理模式，让模型在回答复杂问题时先进行逐步思考，再给出结论。

### 1.2 核心特性
- **触发方式**: 用户显式指定 - `/deep` 或 `深度分析：` 前缀
- **输出格式**: 自然语言流式思考过程，无显式步骤标签
- **实现方式**: 新增 `DEEP_QA` 意图类型

## 2. 架构设计

### 2.1 触发流程

```
用户输入 "/deep Transformer有什么优缺点?"
    ↓
前缀检测 (startsWith("/deep") || startsWith("深度分析："))
    ↓
意图分类返回 DEEP_QA
    ↓
路由到 DEEP_QA_PROMPT_TEMPLATE
    ↓
自然语言深度推理输出
```

### 2.2 文件变更

| 文件 | 操作 |
|------|------|
| `UserIntent.java` | 新增 `DEEP_QA` 枚举值 |
| `PromptConstants.java` | 新增 `DEEP_QA_PROMPT_TEMPLATE`，更新 `INTENT_CLASSIFICATION_PROMPT` |
| `VideoService.java` | 新增 `deepAnalyze()` 接口方法 |
| `VideoServiceImpl.java` | 实现 `deepAnalyze()`，`smartAsk` 中添加 DEEP_QA 分支 |

## 3. 详细设计

### 3.1 Prompt 模板

**DEEP_QA_PROMPT_TEMPLATE:**

```
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
```

### 3.2 UserIntent 枚举扩展

```java
public enum UserIntent {
    SUMMARIZE,
    QA,
    EXTRACT_CONCEPTS,
    EXTRACT_QUOTES,
    SEARCH_KEYWORD,
    DEEP_QA  // 新增
}
```

### 3.3 INTENT_CLASSIFICATION_PROMPT 更新

在意图类型说明中添加：

```
- DEEP_QA: 用户请求深度分析，通常以 /deep 或 深度分析： 开头
  示例: "/deep Transformer有什么优缺点"、"深度分析：视频的核心观点"
```

### 3.4 VideoService 接口

```java
/**
 * 深度分析问答
 * 启用 CoT (思维链) 模式进行推理
 */
String deepAnalyze(String subtitleContent, String question);
```

### 3.5 VideoServiceImpl 实现

```java
@Override
public String deepAnalyze(String subtitleContent, String question) {
    String userPrompt = String.format(
            PromptConstants.DEEP_QA_PROMPT_TEMPLATE,
            subtitleContent,
            question
    );

    return chatClient.prompt()
            .user(userPrompt)
            .call()
            .content();
}
```

smartAsk 方法中添加分支：

```java
case DEEP_QA -> deepAnalyze(subtitleContent, question);
```

## 4. 验收标准

### 4.1 功能测试

| 场景 | 输入 | 预期行为 |
|------|------|----------|
| 前缀触发 `/deep` | `/deep Transformer有什么优缺点` | 返回 DEEP_QA 意图，启用深度推理 |
| 前缀触发 `深度分析：` | `深度分析：视频的核心观点` | 返回 DEEP_QA 意图，启用深度推理 |
| 普通问答 | `Transformer是什么` | 返回 QA 意图，普通问答模式 |

### 4.2 API 测试

```bash
curl -X POST http://localhost:8080/api/video/smart-ask \
  -H "Content-Type: application/json" \
  -d '{
    "subtitleContent": "[00:05:00] Transformer支持并行计算...",
    "question": "/deep 分析Transformer的优缺点"
  }'
```

### 4.3 验收清单

- [ ] `UserIntent` 包含 `DEEP_QA` 枚举值
- [ ] `DEEP_QA_PROMPT_TEMPLATE` 包含 CoT 引导指令
- [ ] 意图分类能正确识别 `/deep` 和 `深度分析：` 前缀
- [ ] `smartAsk` 正确路由到 `deepAnalyze` 方法
- [ ] API 测试返回自然语言的深度分析结果
