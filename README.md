# 长视频智能分析助手

基于 Spring AI Alibaba 构建的长视频字幕智能分析工具。

## 功能特性

- 上传字幕文件（.txt/.srt）
- 一键生成视频总结
- 基于字幕的智能问答

## 技术栈

- Java 17
- Spring Boot 3.2.x
- Spring AI Alibaba
- Thymeleaf
- Maven (使用 Maven Wrapper，无需本地安装)

## 快速开始

### 1. 设置 API Key

```bash
export DASHSCOPE_API_KEY="your-api-key"
```

### 2. 启动应用

```bash
cd LongVideoAgent
./mvnw spring-boot:run
```

> 首次运行会自动下载 Maven，无需本地安装

### 3. 访问应用

打开浏览器访问 http://localhost:8080

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

## 后续扩展

- [ ] Few-shot 示例优化
- [ ] 意图分类 Router
- [ ] RAG 向量检索
- [ ] Function Calling
