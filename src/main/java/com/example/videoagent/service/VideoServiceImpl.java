package com.example.videoagent.service;

import com.example.videoagent.config.PromptConstants;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 视频分析服务实现
 * 使用 Spring AI ChatClient 调用 Qwen 模型
 */
@Service
public class VideoServiceImpl implements VideoService {

    private final ChatClient chatClient;
    private final IntentClassificationService intentClassificationService;

    public VideoServiceImpl(ChatClient.Builder chatClientBuilder,
                           IntentClassificationService intentClassificationService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(PromptConstants.SYSTEM_PROMPT)
                .build();
        this.intentClassificationService = intentClassificationService;
    }

    @Override
    public String summarize(String subtitleContent) {
        String userPrompt = String.format(
                PromptConstants.SUMMARIZE_PROMPT_TEMPLATE,
                subtitleContent
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String chat(String subtitleContent, String question) {
        String userPrompt = String.format(
                PromptConstants.CHAT_PROMPT_TEMPLATE,
                subtitleContent,
                question
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

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

    @Override
    public String extractQuotes(String subtitleContent) {
        String userPrompt = String.format(
                PromptConstants.EXTRACT_QUOTES_PROMPT_TEMPLATE,
                subtitleContent
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String searchKeyword(String subtitleContent, String keyword) {
        String userPrompt = String.format(
                PromptConstants.SEARCH_KEYWORD_PROMPT_TEMPLATE,
                subtitleContent,
                keyword
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

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

    /**
     * 从问题中提取搜索关键词
     * 简单实现：移除常见前缀词
     */
    private String extractKeywordFromQuestion(String question) {
        // 移除常见的前缀
        String keyword = question
                .replace("哪里提到了", "")
                .replace("在什么位置说了", "")
                .replace("搜索", "")
                .replace("查找", "")
                .replace("找到", "")
                .trim();

        // 如果关键词为空，返回原问题
        return keyword.isEmpty() ? question : keyword;
    }
}
