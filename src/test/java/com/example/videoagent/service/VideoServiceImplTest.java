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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
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

    private VideoServiceImpl videoService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";

    @BeforeEach
    void setUp() {
        when(mockBuilder.defaultSystem(any(String.class))).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);
        videoService = new VideoServiceImpl(mockBuilder, mockIntentService);
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
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        assertEquals(expectedAnswer, result);
        verify(mockIntentService).classifyIntentWithCache(question);
    }

    @Test
    @DisplayName("smartAsk - QA 意图路由到 chat 方法")
    void smartAsk_QAIntent_RoutesToChat() {
        // Arrange
        String question = "什么是 RAG？";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);
        String expectedAnswer = "RAG 是检索增强生成";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
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
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert - 验证 prompt 中包含提取的关键词 "Transformer"
        verify(mockRequestSpec).user(contains("Transformer"));
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 移除 '搜索' 前缀")
    void extractKeywordFromQuestion_RemovesSearchPrefix() {
        // Arrange
        String question = "搜索 RAG";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        verify(mockRequestSpec).user(contains("搜索关键词：RAG"));
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 移除 '查找' 前缀")
    void extractKeywordFromQuestion_RemovesFindPrefix() {
        // Arrange
        String question = "查找微服务";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert
        verify(mockRequestSpec).user(contains("搜索关键词：微服务"));
    }

    @Test
    @DisplayName("extractKeywordFromQuestion - 空关键词返回原问题")
    void extractKeywordFromQuestion_EmptyKeyword_ReturnsOriginal() {
        // Arrange
        String question = "搜索";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        String expectedAnswer = "搜索结果";

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        videoService.smartAsk(SAMPLE_SUBTITLE, question);

        // Assert - 关键词为空时使用原问题
        verify(mockRequestSpec).user(contains("搜索关键词：搜索"));
    }

    // ==================== extractQuotes 测试 ====================

    @Test
    @DisplayName("extractQuotes - 返回金句列表")
    void extractQuotes_ReturnsQuotesList() {
        // Arrange
        String expectedAnswer = "[{\"timestamp\": \"00:05:20\", \"quote\": \"金句\"}]";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.extractQuotes(SAMPLE_SUBTITLE);

        // Assert
        assertEquals(expectedAnswer, result);
    }

    // ==================== searchKeyword 测试 ====================

    @Test
    @DisplayName("searchKeyword - 返回搜索结果")
    void searchKeyword_ReturnsSearchResult() {
        // Arrange
        String keyword = "Transformer";
        String expectedAnswer = "{\"keyword\": \"Transformer\", \"occurrences\": []}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedAnswer);

        // Act
        String result = videoService.searchKeyword(SAMPLE_SUBTITLE, keyword);

        // Assert
        assertEquals(expectedAnswer, result);
    }
}
