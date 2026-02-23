package com.example.videoagent.service;

import com.example.videoagent.config.PromptConstants;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 视频分析服务实现
 * 使用 Spring AI ChatClient 调用 Qwen 模型
 */
@Service
public class VideoServiceImpl implements VideoService {

    private final ChatClient chatClient;
    private final IntentClassificationService intentClassificationService;
    private final PromptTemplateService promptTemplateService;

    public VideoServiceImpl(ChatClient.Builder chatClientBuilder,
                           IntentClassificationService intentClassificationService,
                           PromptTemplateService promptTemplateService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(PromptConstants.SYSTEM_PROMPT)
                .build();
        this.intentClassificationService = intentClassificationService;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public String summarize(String subtitleContent) {
        return summarize(subtitleContent, null);
    }

    @Override
    public String summarize(String subtitleContent, String promptVersion) {
        String userPrompt = promptTemplateService.render(
                "summarize",
                promptVersion,
                Map.of("subtitle", subtitleContent)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String chat(String subtitleContent, String question) {
        return chat(subtitleContent, question, null);
    }

    @Override
    public String chat(String subtitleContent, String question, String promptVersion) {
        String userPrompt = promptTemplateService.render(
                "chat",
                promptVersion,
                Map.of("subtitle", subtitleContent, "question", question)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String extractConcepts(String subtitleContent) {
        return extractConcepts(subtitleContent, null);
    }

    @Override
    public String extractConcepts(String subtitleContent, String promptVersion) {
        String userPrompt = promptTemplateService.render(
                "extract-concepts",
                promptVersion,
                Map.of("subtitle", subtitleContent)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String extractQuotes(String subtitleContent) {
        return extractQuotes(subtitleContent, null);
    }

    @Override
    public String extractQuotes(String subtitleContent, String promptVersion) {
        String userPrompt = promptTemplateService.render(
                "extract-quotes",
                promptVersion,
                Map.of("subtitle", subtitleContent)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String searchKeyword(String subtitleContent, String keyword) {
        return searchKeyword(subtitleContent, keyword, null);
    }

    @Override
    public String searchKeyword(String subtitleContent, String keyword, String promptVersion) {
        String userPrompt = promptTemplateService.render(
                "search-keyword",
                promptVersion,
                Map.of("subtitle", subtitleContent, "keyword", keyword)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String smartAsk(String subtitleContent, String question) {
        return smartAsk(subtitleContent, question, null);
    }

    @Override
    public String smartAsk(String subtitleContent, String question, String promptVersion) {
        // Step 1: 意图分类
        IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
        UserIntent intent = intentResult.getIntent();

        // Step 2: 根据意图路由到对应的专用 Prompt
        return switch (intent) {
            case SUMMARIZE -> summarize(subtitleContent, promptVersion);
            case QA -> chat(subtitleContent, question, promptVersion);
            case EXTRACT_CONCEPTS -> extractConcepts(subtitleContent, promptVersion);
            case EXTRACT_QUOTES -> extractQuotes(subtitleContent, promptVersion);
            case SEARCH_KEYWORD -> {
                // 从问题中提取关键词
                String keyword = extractKeywordFromQuestion(question);
                yield searchKeyword(subtitleContent, keyword, promptVersion);
            }
            case DEEP_QA -> deepAnalyze(subtitleContent, question, promptVersion);
        };
    }

    @Override
    public String deepAnalyze(String subtitleContent, String question) {
        return deepAnalyze(subtitleContent, question, null);
    }

    @Override
    public String deepAnalyze(String subtitleContent, String question, String promptVersion) {
        // 移除前缀，获取真实问题
        String realQuestion = question;
        if (question.startsWith("/deep ")) {
            realQuestion = question.substring(6).trim();
        } else if (question.startsWith("深度分析：") || question.startsWith("深度分析:")) {
            realQuestion = question.substring(5).trim();
        }

        String userPrompt = promptTemplateService.render(
                "deep-qa",
                promptVersion,
                Map.of("subtitle", subtitleContent, "question", realQuestion)
        );

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> smartAskStream(String subtitleContent, String question) {
        return smartAskStream(subtitleContent, question, null);
    }

    @Override
    public Flux<String> smartAskStream(String subtitleContent, String question, String promptVersion) {
        // Step 1: 意图分类（复用现有逻辑）
        IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
        UserIntent intent = intentResult.getIntent();

        // Step 2: 根据意图构建 Prompt
        String userPrompt = buildPromptByIntent(subtitleContent, question, intent, promptVersion);

        // Step 3: 流式调用 AI
        return chatClient.prompt()
                .user(userPrompt)
                .stream()
                .content();
    }

    /**
     * 根据意图构建对应的 Prompt
     */
    private String buildPromptByIntent(String subtitleContent, String question, UserIntent intent, String promptVersion) {
        return switch (intent) {
            case SUMMARIZE -> promptTemplateService.render(
                    "summarize", promptVersion, Map.of("subtitle", subtitleContent));
            case QA -> promptTemplateService.render(
                    "chat", promptVersion, Map.of("subtitle", subtitleContent, "question", question));
            case EXTRACT_CONCEPTS -> promptTemplateService.render(
                    "extract-concepts", promptVersion, Map.of("subtitle", subtitleContent));
            case EXTRACT_QUOTES -> promptTemplateService.render(
                    "extract-quotes", promptVersion, Map.of("subtitle", subtitleContent));
            case SEARCH_KEYWORD -> {
                String keyword = extractKeywordFromQuestion(question);
                yield promptTemplateService.render(
                        "search-keyword", promptVersion, Map.of("subtitle", subtitleContent, "keyword", keyword));
            }
            case DEEP_QA -> {
                String realQuestion = question;
                if (question.startsWith("/deep ")) {
                    realQuestion = question.substring(6).trim();
                } else if (question.startsWith("深度分析：") || question.startsWith("深度分析:")) {
                    realQuestion = question.substring(5).trim();
                }
                yield promptTemplateService.render(
                        "deep-qa", promptVersion, Map.of("subtitle", subtitleContent, "question", realQuestion));
            }
        };
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
