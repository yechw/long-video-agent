# 长视频智能分析助手

基于 Spring AI Alibaba 构建的长视频字幕智能分析工具。

## 功能特性

- 上传字幕文件（.txt/.srt）
- 一键生成视频总结
- 基于字幕的智能问答

## 技术栈

- Java 21
- Spring Boot 3.2.x
- Spring AI Alibaba
- Vue 3 + TypeScript + Vite（前端）
- Element Plus（UI 组件库）
- Maven (使用 Maven Wrapper，无需本地安装)

## 快速开始

### 1. 设置 API Key

```bash
export DASHSCOPE_API_KEY="your-api-key"
```

### 2. 启动后端

```bash
cd LongVideoAgent
./mvnw spring-boot:run
```

> 首次运行会自动下载 Maven，无需本地安装

### 3. 启动前端

```bash
cd LongVideoAgent/frontend
npm install
npm run dev
```

### 4. 访问应用

打开浏览器访问 http://localhost:5173

## 使用说明

1. 上传字幕文件或使用示例字幕
2. 点击"生成全局总结"获取视频摘要
3. 在问答区域输入问题，获取 AI 回答

## 学习要点

本项目基于《AI Engineering》第五章 Prompt 工程实践：

- System Prompt 角色设定与约束
- 长文本位置优化（指令在末尾）
- 幻觉限制（只基于提供内容回答）
- 防御性提示（防止 Prompt Injection）

## 已实现功能

- [x] **Phase 1: 基础构建**
  - System Prompt 角色设定与约束
  - 长文本位置优化（指令在末尾）
  - 字幕解析服务

- [x] **Phase 2: 应用最佳实践**
  - 消除歧义与格式化输出（知识点提取）
  - Few-shot 示例优化
  - 意图分类 Router
  - 深度推理（Chain-of-Thought）
  - Markdown 渲染显示
  - 流式输出（SSE）

- [x] **Phase 3: 工程化与防御**
  - Prompt 版本管理（Prompt Library）
  - Prompt Injection 防御
  - Vue 3 前端重构
  - Markdown 格式优化（Prompt 模板 + 流式渲染）

## 后续扩展

- [ ] RAG 向量检索
- [ ] Function Calling
