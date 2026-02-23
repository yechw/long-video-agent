package com.example.videoagent.service;

import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VideoServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceImplTest {

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec mockRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec mockResponseSpec;

    @Mock
    private ChatClient.Builder mockBuilder;

    @Mock
    private IntentClassificationService mockIntentService;

    @Mock
    private PromptTemplateService mockPromptTemplateService;

    private VideoServiceImpl videoService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";
    private static final String RENDERED_PROMPT = "渲染后的 Prompt 内容";

    @BeforeEach
    void setUp() {
        when(mockBuilder.defaultSystem(any(String.class))).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);
        videoService = new VideoServiceImpl(mockBuilder, mockIntentService, mockPromptTemplateService);
    }

    // ==================== smartAsk 路由测试 ====================

    @Test
    @DisplayName("smartAsk - SUMMARIZE 意图路由到 summarize 方法")
    void smartAsk_SummarizeIntent_RoutesToSummarize() {
        // Arrange
        String question = "总结一下";
        IntentResult intentResult = new IntentResult(UserIntent.SUMMARIZE, 0.95);
        String expectedAnswer = "这是总结内容";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("summarize"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockIntentService).classifyIntentWithCache(question);
        verify(mockPromptTemplateService).render(eq("summarize"), any(), anyMap());
    }

    @Test
    @DisplayName("smartAsk - QA 意图路由到 chat 方法")
    void smartAsk_QAIntent_RoutesToChat() {
        // Arrange
        String question = "什么是 RAG？";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);
        String expectedAnswer = "RAG 是检索增强生成";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("chat"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
    }

    @Test
    @DisplayName("smartAsk - EXTRACT_CONCEPTS 意图路由到 extractConcepts 方法")
    void smartAsk_ExtractConceptsIntent_RoutesToExtractConcepts() {
        // Arrange
        String question = "提取知识点";
        IntentResult intentResult = new IntentResult(UserIntent.EXTRACT_CONCEPTS, 0.92);
        String expectedAnswer = "[{\"concept\": \"知识点1\"}]";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("extract-concepts"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
    }

    @Test
    @DisplayName("smartAsk - EXTRACT_QUOTES 意图路由到 extractQuotes 方法")
    void smartAsk_ExtractQuotesIntent_RoutesToExtractQuotes() {
        // Arrange
        String question = "有哪些金句";
        IntentResult intentResult = new IntentResult(UserIntent.EXTRACT_QUOTES, 0.90);
        String expectedAnswer = "[{\"quote\": \"金句1\"}]";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("extract-quotes"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
    }

    @Test
    @DisplayName("smartAsk - SEARCH_KEYWORD 意图路由到 searchKeyword 方法")
    void smartAsk_SearchKeywordIntent_RoutesToSearchKeyword() {
        // Arrange
        String question = "哪里提到了 Transformer";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "{\"keyword\": \"Transformer\", \"occurrences\": []}";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
    }

    // ==================== extractKeywordFromQuestion 测试 ====================

    @Test
    @DisplayName("extractKeywordFromQuestion - 移除 '哪里提到了' 前缀")
    void extractKeywordFromQuestion_RemovesWherMentioned() {
        // Arrange
        String question = "哪里提到了 Transformer";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert - 验证 render 被调用时 keyword 参数为 "Transformer"
        verify(mockPromptTemplateService).render(
                eq("search-keyword"),
                any(),
                argThat(map -> "Transformer".equals(map.get("keyword")))
        );
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 移除 '搜索' 前缀")
    void extractKeywordFromQuestion_RemovesSearchPrefix() {
        // Arrange
        String question = "搜索 RAG";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        verify(mockPromptTemplateService).render(
                eq("search-keyword"),
                any(),
                argThat(map -> "RAG".equals(map.get("keyword")))
        );
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 移除 '查找' 前缀")
    void extractKeywordFromQuestion_RemovesFindPrefix() {
        // Arrange
        String question = "查找微服务";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        verify(mockPromptTemplateService).render(
                eq("search-keyword"),
                any(),
                argThat(map -> "微服务".equals(map.get("keyword")))
        );
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 空关键词返回原问题")
    void extractKeywordFromQuestion_EmptyKeyword_ReturnsOriginal() {
        // Arrange
        String question = "搜索";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert - 关键词为空时使用原问题
        verify(mockPromptTemplateService).render(
                eq("search-keyword"),
                any(),
                argThat(map -> "搜索".equals(map.get("keyword")))
        );
    }

    // ==================== extractQuotes 测试 ====================

    @Test
    @DisplayName("extractQuotes - 返回金句列表")
    void extractQuotes_ReturnsQuotesList() {
        // Arrange
        String expectedAnswer = "[{\"timestamp\": \"00:05:20\", \"quote\": \"金句\"}]";

        when(mockPromptTemplateService.render(eq("extract-quotes"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.extractQuotes(SAMPLE_SUBTITLE);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockPromptTemplateService).render(
                eq("extract-quotes"),
                any(),
                argThat(map -> SAMPLE_SUBTITLE.equals(map.get("subtitle")))
        );
    }

    // ==================== searchKeyword 测试 ====================

    @Test
    @DisplayName("searchKeyword - 返回搜索结果")
    void searchKeyword_ReturnsSearchResult() {
        // Arrange
        String keyword = "Transformer";
        String expectedAnswer = "{\"keyword\": \"Transformer\", \"occurrences\": []}";

        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.searchKeyword(SAMPLE_SUBTITLE, keyword);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockPromptTemplateService).render(
                eq("search-keyword"),
                any(),
                argThat(map -> keyword.equals(map.get("keyword")) && SAMPLE_SUBTITLE.equals(map.get("subtitle")))
        );
    }

    // ==================== promptVersion 参数测试 ====================

    @Test
    @DisplayName("summarize - 使用指定版本渲染 Prompt")
    void summarize_WithPromptVersion_UsesSpecifiedVersion() {
        // Arrange
        String promptVersion = "v2";
        String expectedAnswer = "总结内容";

        when(mockPromptTemplateService.render(eq("summarize"), eq(promptVersion), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.summarize(SAMPLE_SUBTITLE, promptVersion);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockPromptTemplateService).render(eq("summarize"), eq(promptVersion), anyMap());
    }

    @Test
    @DisplayName("chat - 使用指定版本渲染 Prompt")
    void chat_WithPromptVersion_UsesSpecifiedVersion() {
        // Arrange
        String promptVersion = "v2";
        String question = "什么是 RAG？";
        String expectedAnswer = "RAG 是检索增强生成";

        when(mockPromptTemplateService.render(eq("chat"), eq(promptVersion), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.chat(SAMPLE_SUBTITLE, question, promptVersion);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockPromptTemplateService).render(
                eq("chat"),
                eq(promptVersion),
                argThat(map -> SAMPLE_SUBTITLE.equals(map.get("subtitle")) && question.equals(map.get("question")))
        );
    }

    @Test
    @DisplayName("smartAsk - 使用指定版本渲染 Prompt")
    void smartAsk_WithPromptVersion_UsesSpecifiedVersion() {
        // Arrange
        String promptVersion = "v2";
        String question = "总结一下";
        IntentResult intentResult = new IntentResult(UserIntent.SUMMARIZE, 0.95);
        String expectedAnswer = "这是总结内容";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("summarize"), eq(promptVersion), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question, promptVersion);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockPromptTemplateService).render(eq("summarize"), eq(promptVersion), anyMap());
    }
}
