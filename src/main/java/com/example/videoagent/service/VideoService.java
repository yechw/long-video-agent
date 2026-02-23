package com.example.videoagent.service;

import reactor.core.publisher.Flux;

/**
 * 视频分析服务接口
 */
public interface VideoService {

    /**
     * 生成视频总结
     * @param subtitleContent 字幕内容
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return 总结文本
     */
    String summarize(String subtitleContent, String promptVersion);

    /**
     * 生成视频总结（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @return 总结文本
     */
    String summarize(String subtitleContent);

    /**
     * 问答对话
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return AI 回答
     */
    String chat(String subtitleContent, String question, String promptVersion);

    /**
     * 问答对话（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return AI 回答
     */
    String chat(String subtitleContent, String question);

    /**
     * 提取知识点
     * @param subtitleContent 字幕内容
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return JSON 格式的知识点列表
     */
    String extractConcepts(String subtitleContent, String promptVersion);

    /**
     * 提取知识点（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @return JSON 格式的知识点列表
     */
    String extractConcepts(String subtitleContent);

    /**
     * 提取金句
     * @param subtitleContent 字幕内容
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return JSON 格式的金句列表
     */
    String extractQuotes(String subtitleContent, String promptVersion);

    /**
     * 提取金句（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @return JSON 格式的金句列表
     */
    String extractQuotes(String subtitleContent);

    /**
     * 搜索关键词
     * @param subtitleContent 字幕内容
     * @param keyword 关键词
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return JSON 格式的搜索结果
     */
    String searchKeyword(String subtitleContent, String keyword, String promptVersion);

    /**
     * 搜索关键词（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @param keyword 关键词
     * @return JSON 格式的搜索结果
     */
    String searchKeyword(String subtitleContent, String keyword);

    /**
     * 智能问答（自动意图分类 + 路由执行）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return 回答内容
     */
    String smartAsk(String subtitleContent, String question, String promptVersion);

    /**
     * 智能问答（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return 回答内容
     */
    String smartAsk(String subtitleContent, String question);

    /**
     * 深度分析问答
     * 启用 CoT (思维链) 模式进行推理
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return 深度分析回答
     */
    String deepAnalyze(String subtitleContent, String question, String promptVersion);

    /**
     * 深度分析问答（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return 深度分析回答
     */
    String deepAnalyze(String subtitleContent, String question);

    /**
     * 智能问答流式输出
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @param promptVersion Prompt 版本号（可选，null 使用默认版本）
     * @return 流式回答内容
     */
    Flux<String> smartAskStream(String subtitleContent, String question, String promptVersion);

    /**
     * 智能问答流式输出（使用默认 Prompt 版本）
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return 流式回答内容
     */
    Flux<String> smartAskStream(String subtitleContent, String question);
}
