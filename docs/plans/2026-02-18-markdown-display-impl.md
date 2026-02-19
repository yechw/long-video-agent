# Markdown 格式化展示 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为智能问答和总结添加 Markdown 渲染，使用 marked.js + highlight.js

**Architecture:** 客户端渲染，通过 CDN 引入 marked.js 和 highlight.js，修改 Thymeleaf 模板和 CSS

**Tech Stack:** marked.js, highlight.js, Thymeleaf, CSS

---

## Task 1: 添加 CDN 引用和渲染脚本

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: 在 `<head>` 中添加 CDN 引用**

在 `</head>` 标签前添加：

```html
    <!-- Markdown 渲染 -->
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <!-- 代码高亮 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/highlight.js@11/styles/github-dark.min.css">
    <script src="https://cdn.jsdelivr.net/npm/highlight.js@11"></script>
```

**Step 2: 在 `</body>` 前添加渲染脚本**

在 `</body>` 标签前添加：

```html
<script>
document.addEventListener('DOMContentLoaded', function() {
    // 配置 marked
    marked.setOptions({
        highlight: function(code, lang) {
            if (lang && hljs.getLanguage(lang)) {
                return hljs.highlight(code, { language: lang }).value;
            }
            return hljs.highlightAuto(code).value;
        },
        breaks: true,
        gfm: true
    });

    // 渲染所有 markdown-content
    document.querySelectorAll('.markdown-content').forEach(function(el) {
        const content = el.getAttribute('data-content');
        if (content) {
            const rendered = el.querySelector('.markdown-rendered');
            rendered.innerHTML = marked.parse(content);
        }
    });
});
</script>
```

**Step 3: 验证应用启动**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run -q`
Expected: 应用启动成功

---

## Task 2: 修改智能问答和总结的渲染区域

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: 修改智能问答结果区域**

找到以下代码块（约第 79-92 行）：

```html
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

替换为：

```html
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
        <div class="markdown-content" th:data-content="${smartAnswer}">
            <div class="markdown-rendered"></div>
        </div>
    </div>
</section>
```

**Step 2: 修改视频总结区域**

找到以下代码块（约第 94-99 行）：

```html
<section class="result-section" th:if="${summary}">
    <h2>📝 视频总结</h2>
    <div class="result-card">
        <pre th:text="${summary}"></pre>
    </div>
</section>
```

替换为：

```html
<section class="result-section" th:if="${summary}">
    <h2>📝 视频总结</h2>
    <div class="result-card">
        <div class="markdown-content" th:data-content="${summary}">
            <div class="markdown-rendered"></div>
        </div>
    </div>
</section>
```

**Step 3: 验证应用启动**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run -q`
Expected: 应用启动成功

---

## Task 3: 添加 Markdown 样式

**Files:**
- Modify: `src/main/resources/static/style.css`

**Step 1: 在文件末尾添加 Markdown 样式**

在 `style.css` 末尾添加：

```css
/* Markdown 内容样式 */
.markdown-content {
    line-height: 1.8;
    color: #333;
}

.markdown-rendered {
    white-space: normal;
}

/* 标题样式 */
.markdown-rendered h1,
.markdown-rendered h2,
.markdown-rendered h3 {
    color: #333;
    margin: 1.5em 0 0.8em;
    padding-bottom: 0.3em;
    border-bottom: 1px solid #e0e0e0;
}

.markdown-rendered h1 { font-size: 1.5em; }
.markdown-rendered h2 { font-size: 1.3em; }
.markdown-rendered h3 { font-size: 1.1em; }

/* 段落 */
.markdown-rendered p {
    margin: 0.8em 0;
}

/* 列表 */
.markdown-rendered ul,
.markdown-rendered ol {
    margin: 0.8em 0;
    padding-left: 2em;
}

.markdown-rendered li {
    margin: 0.4em 0;
}

/* 代码块 */
.markdown-rendered pre {
    background: #1e1e1e;
    border-radius: 8px;
    padding: 1em;
    overflow-x: auto;
    margin: 1em 0;
    border: 1px solid #ddd;
}

.markdown-rendered code {
    font-family: 'Fira Code', 'Consolas', monospace;
    font-size: 0.9em;
}

/* 行内代码 */
.markdown-rendered p code {
    background: #f0f0f0;
    padding: 0.2em 0.4em;
    border-radius: 4px;
    color: #c7254e;
}

/* 引用块 */
.markdown-rendered blockquote {
    border-left: 4px solid #667eea;
    margin: 1em 0;
    padding: 0.5em 1em;
    background: #f8f9fa;
    color: #666;
}

/* 链接 */
.markdown-rendered a {
    color: #667eea;
    text-decoration: none;
}

.markdown-rendered a:hover {
    text-decoration: underline;
}

/* 表格 */
.markdown-rendered table {
    width: 100%;
    border-collapse: collapse;
    margin: 1em 0;
}

.markdown-rendered th,
.markdown-rendered td {
    border: 1px solid #ddd;
    padding: 0.6em 1em;
    text-align: left;
}

.markdown-rendered th {
    background: #f8f9fa;
    font-weight: bold;
}

/* 分割线 */
.markdown-rendered hr {
    border: none;
    border-top: 1px solid #e0e0e0;
    margin: 1.5em 0;
}
```

**Step 2: 验证应用启动**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run -q`
Expected: 应用启动成功

---

## Task 4: 验证并提交

**Step 1: 启动应用**

Run: `cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent && mvn spring-boot:run`
Expected: 应用启动成功

**Step 2: 浏览器测试**

1. 打开 http://localhost:8080
2. 点击"使用示例字幕"
3. 点击"生成全局总结" - 应看到 Markdown 渲染效果
4. 在智能问答输入问题 - 应看到 Markdown 渲染效果
5. 检查代码块是否有语法高亮

**Step 3: 提交代码**

```bash
git add src/main/resources/templates/index.html \
        src/main/resources/static/style.css

git commit -m "feat: add markdown rendering for AI responses

- Add marked.js + highlight.js via CDN
- Render smartAsk and summarize responses as Markdown
- Add CSS styles for markdown content
- Keep regular chat as plain text

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验收清单

- [ ] CDN 资源正确加载（浏览器控制台无错误）
- [ ] 智能问答结果正确渲染 Markdown
- [ ] 总结结果正确渲染 Markdown
- [ ] 代码块有语法高亮
- [ ] 普通问答 (`/chat`) 保持纯文本展示
- [ ] 样式美观，阅读体验良好
