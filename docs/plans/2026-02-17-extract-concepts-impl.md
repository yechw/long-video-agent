# 提取知识点功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增"提取知识点"功能，从字幕中提取结构化知识点，前端以时间线形式展示。

**Architecture:** 新增 Concept DTO、扩展 VideoService 接口、添加 /extract 端点、前端时间线组件。

**Tech Stack:** Java 17, Spring Boot 3.2.x, Jackson ObjectMapper, Thymeleaf, CSS

---

## Task 1: 添加 Jackson 依赖

**Files:**
- Modify: `pom.xml`

**Step 1: 添加 Jackson 依赖（如果尚未存在）**

在 `pom.xml` 的 `<dependencies>` 中确认/添加：

```xml
<!-- Jackson for JSON parsing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**Step 2: 验证依赖**

```bash
./mvnw dependency:resolve -q | grep jackson
```

Expected: 显示 jackson-databind 依赖

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: ensure jackson-databind dependency for JSON parsing"
```

---

## Task 2: 创建 Concept DTO

**Files:**
- Create: `src/main/java/com/example/videoagent/dto/Concept.java`

**Step 1: 创建 Concept.java**

```java
package com.example.videoagent.dto;

/**
 * 知识点数据传输对象
 */
public class Concept {

    private String timestampFrom;
    private String timestampTo;
    private String concept;
    private String description;

    public Concept() {}

    public Concept(String timestampFrom, String timestampTo, String concept, String description) {
        this.timestampFrom = timestampFrom;
        this.timestampTo = timestampTo;
        this.concept = concept;
        this.description = description;
    }

    public String getTimestampFrom() {
        return timestampFrom;
    }

    public void setTimestampFrom(String timestampFrom) {
        this.timestampFrom = timestampFrom;
    }

    public String getTimestampTo() {
        return timestampTo;
    }

    public void setTimestampTo(String timestampTo) {
        this.timestampTo = timestampTo;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

**Step 2: 验证编译**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/dto/Concept.java
git commit -m "feat: add Concept DTO for knowledge point extraction"
```

---

## Task 3: 添加提取知识点 Prompt 模板

**Files:**
- Modify: `src/main/java/com/example/videoagent/config/PromptConstants.java`

**Step 1: 在 PromptConstants.java 中添加新常量**

在类的末尾（最后一个 `}` 之前）添加：

```java
    /**
     * 提取知识点 Prompt 模板
     * 输出格式：JSON 数组
     */
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

**Step 2: 验证编译**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/config/PromptConstants.java
git commit -m "feat: add EXTRACT_CONCEPTS_PROMPT_TEMPLATE for structured JSON output"
```

---

## Task 4: 扩展 VideoService 接口

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoService.java`

**Step 1: 添加接口方法**

在 `VideoService.java` 接口中添加新方法：

```java
    /**
     * 提取知识点
     * @param subtitleContent 字幕内容
     * @return JSON 格式的知识点列表
     */
    String extractConcepts(String subtitleContent);
```

**Step 2: 验证编译（会失败，因为实现类还没实现）**

```bash
./mvnw compile -q
```

Expected: 编译成功（接口不需要实现）

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/service/VideoService.java
git commit -m "feat: add extractConcepts method to VideoService interface"
```

---

## Task 5: 实现 VideoServiceImpl

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoServiceImpl.java`

**Step 1: 实现 extractConcepts 方法**

在 `VideoServiceImpl.java` 类的末尾（最后一个 `}` 之前）添加：

```java
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

**Step 2: 验证编译**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/service/VideoServiceImpl.java
git commit -m "feat: implement extractConcepts in VideoServiceImpl"
```

---

## Task 6: 添加 Controller 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoController.java`

**Step 1: 添加 import 语句**

在文件顶部的 import 区域添加：

```java
import com.example.videoagent.dto.Concept;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
```

**Step 2: 添加 /extract 端点和辅助方法**

在类的末尾（最后一个 `}` 之前）添加：

```java
    /**
     * 提取知识点
     */
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

    /**
     * 从 AI 响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }
```

**Step 3: 验证编译**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/com/example/videoagent/controller/VideoController.java
git commit -m "feat: add /extract endpoint with JSON parsing"
```

---

## Task 7: 更新前端按钮布局

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: 修改操作区域**

找到 `action-section` 部分，替换为：

```html
        <!-- 操作区域 -->
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

**Step 2: Commit**

```bash
git add src/main/resources/templates/index.html
git commit -m "feat: add extract concepts button to action section"
```

---

## Task 8: 添加时间线展示组件

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: 在问答区域之前添加时间线组件**

在 `<!-- 问答区域 -->` 注释之前添加：

```html
        <!-- 知识点时间线 -->
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

**Step 2: Commit**

```bash
git add src/main/resources/templates/index.html
git commit -m "feat: add timeline component for knowledge points display"
```

---

## Task 9: 添加时间线 CSS 样式

**Files:**
- Modify: `src/main/resources/static/style.css`

**Step 1: 在 CSS 文件末尾添加时间线样式**

```css
/* 时间线样式 */
.timeline-section {
    margin-top: 20px;
}

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

.timeline-time {
    font-size: 0.85em;
    color: #667eea;
    font-weight: 500;
    margin-bottom: 8px;
}

.time-arrow {
    margin: 0 5px;
}

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

/* 响应式时间线 */
@media (max-width: 600px) {
    .timeline {
        padding-left: 25px;
    }

    .timeline-item::before {
        left: -18px;
        width: 8px;
        height: 8px;
    }
}
```

**Step 2: Commit**

```bash
git add src/main/resources/static/style.css
git commit -m "feat: add timeline CSS styles for knowledge points"
```

---

## Task 10: 集成测试与验证

**Files:**
- None (testing only)

**Step 1: 编译项目**

```bash
./mvnw compile -q
```

Expected: BUILD SUCCESS

**Step 2: 启动应用**

```bash
export DASHSCOPE_API_KEY="your-api-key"
./mvnw spring-boot:run
```

Expected: 应用在 8080 端口启动成功

**Step 3: 功能测试清单**

| 测试项 | 操作 | 预期结果 |
|--------|------|----------|
| 1. 首页访问 | 打开 http://localhost:8080 | 显示上传页面 |
| 2. 加载示例 | 点击"使用示例字幕" | 显示字幕已加载 |
| 3. 提取知识点 | 点击"提取知识点" | 显示时间线，包含知识点卡片 |
| 4. 时间线展示 | 查看时间线 | 每个节点显示时间范围、名称、描述 |
| 5. 错误处理 | 模拟错误场景 | 显示错误提示，不崩溃 |

**Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete Phase 2 Task 3 - extract concepts with timeline display

- Add Concept DTO for structured knowledge points
- Add EXTRACT_CONCEPTS_PROMPT_TEMPLATE for JSON output
- Implement /extract endpoint with JSON parsing
- Add timeline component and CSS styles

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 实现完成检查清单

- [ ] Task 1: 确认 Jackson 依赖
- [ ] Task 2: 创建 Concept DTO
- [ ] Task 3: 添加 Prompt 模板
- [ ] Task 4: 扩展 VideoService 接口
- [ ] Task 5: 实现 VideoServiceImpl
- [ ] Task 6: 添加 Controller 端点
- [ ] Task 7: 更新前端按钮布局
- [ ] Task 8: 添加时间线展示组件
- [ ] Task 9: 添加时间线 CSS 样式
- [ ] Task 10: 集成测试与验证
