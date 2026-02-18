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
