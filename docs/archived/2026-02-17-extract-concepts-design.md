# Phase 2 任务3：消除歧义与格式化输出 - 设计文档

> 创建日期：2026-02-17
> 状态：已批准
> 所属阶段：Phase 2 - 应用最佳实践 (5.2 节)

## 1. 需求概述

### 1.1 目标
新增"提取知识点"功能，返回结构化 JSON 数据，前端以时间线形式展示。

### 1.2 核心功能
| 功能 | 说明 |
|------|------|
| 提取知识点 | 从字幕中提取 5-10 个核心知识点 |
| JSON 输出 | 强制结构化输出，便于前端渲染 |
| 时间线展示 | 按时间轴可视化知识点 |

### 1.3 需求确认

| 项目 | 决定 |
|------|------|
| JSON 字段 | `timestampFrom`, `timestampTo`, `concept`, `description` |
| 前端展示 | 时间线展示 |
| 触发方式 | 独立按钮（在"生成全局总结"旁边） |

## 2. JSON 格式设计

### 2.1 单个知识点结构
```json
{
  "timestampFrom": "00:00:05",
  "timestampTo": "00:00:20",
  "concept": "提示工程",
  "description": "提示工程是构建 AI 应用的核心技能，通过有效沟通让模型生成期望输出"
}
```

### 2.2 完整输出示例
```json
[
  {
    "timestampFrom": "00:00:00",
    "timestampTo": "00:00:12",
    "concept": "AI工程课程",
    "description": "本课程介绍 AI 工程的核心概念和实践技能"
  },
  {
    "timestampFrom": "00:00:12",
    "timestampTo": "00:00:30",
    "concept": "提示工程",
    "description": "提示工程是构建 AI 应用的核心技能"
  },
  {
    "timestampFrom": "00:00:30",
    "timestampTo": "00:00:50",
    "concept": "System Prompt",
    "description": "系统提示用于设定 AI 的角色和行为边界"
  }
]
```

## 3. 后端设计

### 3.1 文件变更

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `config/PromptConstants.java` | 修改 | 新增 EXTRACT_CONCEPTS_PROMPT_TEMPLATE |
| `dto/Concept.java` | 新增 | 知识点 DTO 类 |
| `service/VideoService.java` | 修改 | 新增 extractConcepts() 接口方法 |
| `service/VideoServiceImpl.java` | 修改 | 实现 extractConcepts() |
| `controller/VideoController.java` | 修改 | 新增 /extract 端点 |

### 3.2 Prompt 模板

```java
public static final String EXTRACT_CONCEPTS_PROMPT_TEMPLATE = """
        %s

        ---
        基于以上视频字幕内容，提取 5-10 个核心知识点。

        【输出格式要求】
        必须输出纯 JSON 数组，不要包含任何开场白或结束语。
        每个知识点包含以下字段：
        - timestampFrom: 知识点开始时间（格式: HH:MM:SS）
        - timestampTo: 知识点结束时间（格式: HH:MM:SS）
        - concept: 知识点名称（简短，不超过10字）
        - description: 知识点描述（1-2句话说明）

        【示例输出】
        [
          {
            "timestampFrom": "00:00:05",
            "timestampTo": "00:00:20",
            "concept": "提示工程",
            "description": "提示工程是构建 AI 应用的核心技能"
          }
        ]
        """;
```

### 3.3 Concept DTO

```java
package com.example.videoagent.dto;

public class Concept {
    private String timestampFrom;
    private String timestampTo;
    private String concept;
    private String description;

    // getters, setters, constructors
}
```

### 3.4 Service 方法

```java
// VideoService.java
String extractConcepts(String subtitleContent);

// VideoServiceImpl.java
@Override
public String extractConcepts(String subtitleContent) {
    String userPrompt = String.format(
            PromptConstants.EXTRACT_CONCEPTS_PROMPT_TEMPLATE,
            subtitleContent
    );
    return chatClient.prompt()
            .user(userPrompt)
            .call()
            .content();
}
```

### 3.5 Controller 端点

```java
@PostMapping("/extract")
public String extractConcepts(
        @RequestParam("subtitleContent") String subtitleContent,
        Model model) {

    try {
        String jsonResponse = videoService.extractConcepts(subtitleContent);

        // 解析 JSON 为 List<Concept>
        ObjectMapper mapper = new ObjectMapper();
        String jsonArray = extractJsonArray(jsonResponse);
        List<Concept> concepts = mapper.readValue(jsonArray,
            new TypeReference<List<Concept>>(){});

        model.addAttribute("subtitleLoaded", true);
        model.addAttribute("subtitleContent", subtitleContent);
        model.addAttribute("concepts", concepts);
    } catch (Exception e) {
        model.addAttribute("error", "提取知识点失败: " + e.getMessage());
        model.addAttribute("subtitleLoaded", true);
        model.addAttribute("subtitleContent", subtitleContent);
    }

    return "index";
}

// 辅助方法：从响应中提取 JSON 数组
private String extractJsonArray(String response) {
    int start = response.indexOf('[');
    int end = response.lastIndexOf(']');
    if (start >= 0 && end > start) {
        return response.substring(start, end + 1);
    }
    return "[]";
}
```

## 4. 前端设计

### 4.1 按钮布局

```html
<section class="action-section" th:if="${subtitleLoaded}">
    <h2>📊 快捷操作</h2>
    <div class="button-group">
        <form action="/summarize" method="post" style="display:inline">
            <input type="hidden" name="subtitleContent" th:value="${subtitleContent}">
            <button type="submit" class="btn btn-primary">生成全局总结</button>
        </form>
        <form action="/extract" method="post" style="display:inline">
            <input type="hidden" name="subtitleContent" th:value="${subtitleContent}">
            <button type="submit" class="btn btn-secondary">提取知识点</button>
        </form>
    </div>
</section>
```

### 4.2 时间线组件

```html
<section class="timeline-section" th:if="${concepts}">
    <h2>📚 知识点时间线</h2>
    <div class="timeline">
        <div class="timeline-item" th:each="concept : ${concepts}">
            <div class="timeline-time">
                <span th:text="${concept.timestampFrom}">00:00:00</span>
                <span class="time-arrow">→</span>
                <span th:text="${concept.timestampTo}">00:00:12</span>
            </div>
            <div class="timeline-content">
                <h3 class="concept-name" th:text="${concept.concept}">知识点名称</h3>
                <p class="concept-desc" th:text="${concept.description}">知识点描述</p>
            </div>
        </div>
    </div>
</section>
```

### 4.3 CSS 样式

```css
/* 时间线容器 */
.timeline {
    position: relative;
    padding-left: 30px;
}

.timeline::before {
    content: '';
    position: absolute;
    left: 10px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: linear-gradient(to bottom, #667eea, #764ba2);
}

/* 时间线项 */
.timeline-item {
    position: relative;
    margin-bottom: 20px;
    padding: 15px;
    background: #f8f9fa;
    border-radius: 8px;
    border-left: 3px solid #667eea;
}

.timeline-item::before {
    content: '';
    position: absolute;
    left: -23px;
    top: 20px;
    width: 10px;
    height: 10px;
    background: #667eea;
    border-radius: 50%;
}

/* 时间显示 */
.timeline-time {
    font-size: 0.85em;
    color: #667eea;
    font-weight: 500;
    margin-bottom: 8px;
}

.time-arrow {
    margin: 0 5px;
}

/* 知识点内容 */
.concept-name {
    font-size: 1.1em;
    color: #333;
    margin-bottom: 5px;
}

.concept-desc {
    font-size: 0.9em;
    color: #666;
    line-height: 1.5;
}
```

## 5. 错误处理

### 5.1 错误场景

| 场景 | 处理方式 |
|------|----------|
| AI 返回非 JSON | try-catch 解析异常，显示错误提示 |
| AI 返回空数组 | 正常显示，时间线为空 |
| AI 超时 | 显示"请求超时，请重试" |
| 网络错误 | 显示"网络异常，请检查连接" |

### 5.2 JSON 解析容错

- 使用正则/字符串查找提取 JSON 数组部分
- 配置 ObjectMapper 宽松模式
- 空结果返回空列表而非 null

## 6. 数据流

```
用户点击"提取知识点"
        ↓
POST /extract (subtitleContent)
        ↓
VideoController.extractConcepts()
        ↓
VideoService.extractConcepts() → 调用 AI
        ↓
AI 返回 JSON 字符串
        ↓
extractJsonArray() 提取纯 JSON
        ↓
ObjectMapper 解析 → List<Concept>
        ↓
Model.addAttribute("concepts", concepts)
        ↓
Thymeleaf 渲染时间线
```

## 7. 测试验证

| 测试项 | 预期结果 |
|--------|----------|
| 点击"提取知识点"按钮 | 显示 loading，然后展示时间线 |
| 时间线显示 | 按时间顺序排列，每个节点显示时间范围、知识点名称、描述 |
| JSON 解析失败 | 显示错误提示，不崩溃 |
| 空字幕 | 正常处理，无知识点 |
