package com.example.videoagent.service;

import com.example.videoagent.config.PromptConstants;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 意图分类服务
 * 使用 LLM 进行意图识别，带缓存
 */
@Service
public class IntentClassificationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // Caffeine 缓存：问题 -> 意图结果
    private final Cache<String, IntentResult> intentCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    // 置信度阈值，低于此值默认走 QA
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    public IntentClassificationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 带缓存的意图分类
     */
    public IntentResult classifyIntentWithCache(String question) {
        // 1. 尝试从缓存获取
        IntentResult cached = intentCache.getIfPresent(question);
        if (cached != null) {
            return cached;
        }

        // 2. 调用 LLM 分类
        IntentResult result = classifyIntent(question);

        // 3. 存入缓存
        intentCache.put(question, result);

        return result;
    }

    /**
     * 调用 LLM 进行意图分类
     */
    public IntentResult classifyIntent(String question) {
        String prompt = String.format(PromptConstants.INTENT_CLASSIFICATION_PROMPT, question);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseIntentResult(response);
    }

    /**
     * 解析 LLM 返回的意图结果
     */
    private IntentResult parseIntentResult(String response) {
        try {
            // 提取 JSON 对象
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String json = response.substring(start, end + 1);

                // 解析 JSON
                var node = objectMapper.readTree(json);
                String intentStr = node.get("intent").asText();
                double confidence = node.get("confidence").asDouble();

                UserIntent intent = UserIntent.valueOf(intentStr);

                // 置信度低于阈值，默认走 QA
                if (confidence < CONFIDENCE_THRESHOLD) {
                    return new IntentResult(UserIntent.QA, confidence);
                }

                return new IntentResult(intent, confidence);
            }
        } catch (Exception e) {
            // 解析失败，默认走 QA
        }

        return new IntentResult(UserIntent.QA, 0.5);
    }
}
