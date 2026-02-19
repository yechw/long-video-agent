# Markdown 格式化展示设计文档

> 创建日期: 2026-02-18
> 状态: 待实现

## 1. 概述

### 1.1 目标
让 AI 回复以 Markdown 格式展示，通过前端渲染提供优雅的阅读体验。

### 1.2 核心特性
- **渲染库**: marked.js + highlight.js (CDN 引入)
- **格式化范围**: 智能问答 + 总结（普通问答保持纯文本）
- **实现方式**: 客户端渲染，后端无需改动

## 2. 架构设计

### 2.1 技术栈

| 组件 | 用途 |
|------|------|
| marked.js | Markdown 转 HTML |
| highlight.js | 代码语法高亮 |
| Thymeleaf utext | 安全渲染 HTML |

### 2.2 渲染流程

```
用户提问 → 后端返回 Markdown 文本 → 前端 JS 调用 marked() 转换 →
highlight.js 高亮代码 → th:utext 安全渲染 HTML
```

### 2.3 文件变更

| 文件 | 操作 |
|------|------|
| `templates/index.html` | 添加 CDN 引用、修改渲染区域、添加 JS 脚本 |
| `static/style.css` | 添加 Markdown 展示样式 |

## 3. 详细设计

### 3.1 CDN 引入

在 `templates/index.html` 的 `<head>` 中添加：

```html
<!-- Markdown 渲染 -->
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<!-- 代码高亮 -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/highlight.js@11/styles/github-dark.min.css">
<script src="https://cdn.jsdelivr.net/npm/highlight.js@11"></script>
```

### 3.2 HTML 结构修改

**智能问答结果区域：**

```html
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

**视频总结区域：**

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

### 3.3 JavaScript 渲染逻辑

在 `</body>` 前添加：

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

### 3.4 CSS 样式

在 `static/style.css` 中添加：

```css
/* Markdown 内容样式 */
.markdown-content {
    line-height: 1.8;
    color: #e0e0e0;
}

.markdown-rendered {
    white-space: normal;
}

/* 标题样式 */
.markdown-rendered h1,
.markdown-rendered h2,
.markdown-rendered h3 {
    color: #fff;
    margin: 1.5em 0 0.8em;
    padding-bottom: 0.3em;
    border-bottom: 1px solid #444;
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
    border: 1px solid #333;
}

.markdown-rendered code {
    font-family: 'Fira Code', 'Consolas', monospace;
    font-size: 0.9em;
}

/* 行内代码 */
.markdown-rendered p code {
    background: #333;
    padding: 0.2em 0.4em;
    border-radius: 4px;
    color: #f8f8f2;
}

/* 引用块 */
.markdown-rendered blockquote {
    border-left: 4px solid #4a9eff;
    margin: 1em 0;
    padding: 0.5em 1em;
    background: rgba(74, 158, 255, 0.1);
    color: #b0b0b0;
}

/* 链接 */
.markdown-rendered a {
    color: #4a9eff;
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
    border: 1px solid #444;
    padding: 0.6em 1em;
    text-align: left;
}

.markdown-rendered th {
    background: #2a2a2a;
    font-weight: bold;
}

/* 分割线 */
.markdown-rendered hr {
    border: none;
    border-top: 1px solid #444;
    margin: 1.5em 0;
}
```

## 4. 渲染范围

| 功能 | 渲染方式 | 说明 |
|------|----------|------|
| 智能问答 (`/ask`) | Markdown 渲染 | 支持代码高亮 |
| 总结 (`/summarize`) | Markdown 渲染 | 支持代码高亮 |
| 普通问答 (`/chat`) | 纯文本 (`<pre>`) | 保持不变 |
| 知识点提取 | 时间线组件 | 保持不变 |

## 5. 验收标准

- [ ] CDN 资源正确加载
- [ ] 智能问答结果正确渲染 Markdown
- [ ] 总结结果正确渲染 Markdown
- [ ] 代码块有语法高亮
- [ ] 普通问答保持纯文本展示
- [ ] 样式美观，阅读体验良好
