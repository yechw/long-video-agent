package com.example.videoagent.service;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.example.videoagent.dto.PromptOptimizeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptOptimizeService {

    private final ChatClient chatClient;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public PromptOptimizeService(ChatClient.Builder chatClientBuilder,
                                  PromptTemplateService promptTemplateService,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    public PromptOptimizeResponse optimize(PromptOptimizeRequest request) {
        String goalDescription = buildGoalDescription(
            request.getOptimizationGoal(),
            request.getCustomGoal()
        );

        String metaPrompt = promptTemplateService.render(
            "meta-optimize",
            null,
            Map.of(
                "original_prompt", request.getOriginalPrompt(),
                "goal", goalDescription
            )
        );

        String responseJson = chatClient.prompt()
            .user(metaPrompt)
            .call()
            .content();

        return parseResponse(responseJson);
    }

    private String buildGoalDescription(String goal, String customGoal) {
        return switch (goal.toUpperCase()) {
            case "CLEARER" -> "消除模糊表达，使用明确的动词和结构，添加具体的输入输出格式说明";
            case "CONCISE" -> "精简表达，去除重复和多余修饰词，合并冗余句子，减少 Token 使用量";
            case "STRICT" -> "添加强制性约束，明确要求基于上下文回答，禁止编造，增强防幻觉能力";
            case "COMPLETE" -> "补充 Few-Shot 示例，定义输出格式，添加边界情况处理说明";
            case "CUSTOM" -> customGoal != null ? customGoal : "根据用户需求进行优化";
            default -> "进行全面优化，提升 Prompt 质量和效果";
        };
    }

    private PromptOptimizeResponse parseResponse(String responseJson) {
        try {
            // 清理可能的 markdown 代码块标记
            String cleanJson = responseJson
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

            return objectMapper.readValue(cleanJson, PromptOptimizeResponse.class);
        } catch (Exception e) {
            // 如果解析失败，返回原始内容作为优化结果
            PromptOptimizeResponse fallback = new PromptOptimizeResponse();
            fallback.setOptimizedPrompt(responseJson);
            fallback.setImprovements(java.util.List.of("AI 返回格式异常，请手动检查结果"));
            return fallback;
        }
    }
}
