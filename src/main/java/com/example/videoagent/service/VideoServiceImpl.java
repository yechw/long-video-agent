package com.example.videoagent.service;

import com.example.videoagent.config.PromptConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 视频分析服务实现
 * 使用 Spring AI ChatClient 调用 Qwen 模型
 */
@Service
public class VideoServiceImpl implements VideoService {

    private final ChatClient chatClient;

    public VideoServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(PromptConstants.SYSTEM_PROMPT)
                .build();
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
}
