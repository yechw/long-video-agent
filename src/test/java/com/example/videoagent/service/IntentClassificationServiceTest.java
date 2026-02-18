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
import static org.mockito.Mockito.*;

/**
 * IntentClassificationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class IntentClassificationServiceTest {

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec mockRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec mockResponseSpec;

    @Mock
    private ChatClient.Builder mockBuilder;

    private IntentClassificationService service;

    @BeforeEach
    void setUp() {
        when(mockBuilder.build()).thenReturn(mockChatClient);
        service = new IntentClassificationService(mockBuilder);
    }

    @Test
    @DisplayName("classifyIntent - SUMMARIZE 意图分类")
    void classifyIntent_Summarize() {
        // Arrange
        String question = "总结一下这个视频";
        String llmResponse = "{\"intent\": \"SUMMARIZE\", \"confidence\": 0.95}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.SUMMARIZE, result.getIntent());
        assertEquals(0.95, result.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("classifyIntent - QA 意图分类")
    void classifyIntent_QA() {
        // Arrange
        String question = "什么是 RAG？";
        String llmResponse = "{\"intent\": \"QA\", \"confidence\": 0.88}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.QA, result.getIntent());
        assertEquals(0.88, result.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("classifyIntent - EXTRACT_CONCEPTS 意图分类")
    void classifyIntent_ExtractConcepts() {
        // Arrange
        String question = "提取知识点";
        String llmResponse = "{\"intent\": \"EXTRACT_CONCEPTS\", \"confidence\": 0.92}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.EXTRACT_CONCEPTS, result.getIntent());
    }

    @Test
    @DisplayName("classifyIntent - EXTRACT_QUOTES 意图分类")
    void classifyIntent_ExtractQuotes() {
        // Arrange
        String question = "有哪些金句";
        String llmResponse = "{\"intent\": \"EXTRACT_QUOTES\", \"confidence\": 0.90}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.EXTRACT_QUOTES, result.getIntent());
    }

    @Test
    @DisplayName("classifyIntent - SEARCH_KEYWORD 意图分类")
    void classifyIntent_SearchKeyword() {
        // Arrange
        String question = "哪里提到了 Transformer";
        String llmResponse = "{\"intent\": \"SEARCH_KEYWORD\", \"confidence\": 0.85}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.SEARCH_KEYWORD, result.getIntent());
    }

    @Test
    @DisplayName("classifyIntent - 置信度低于阈值时返回 QA")
    void classifyIntent_LowConfidence_ReturnsQA() {
        // Arrange
        String question = "不确定的问题";
        String llmResponse = "{\"intent\": \"SUMMARIZE\", \"confidence\": 0.5}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert - 置信度低于 0.6 阈值，应返回 QA
        assertEquals(UserIntent.QA, result.getIntent());
        assertEquals(0.5, result.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("classifyIntent - LLM 返回无效 JSON 时返回默认 QA")
    void classifyIntent_InvalidJson_ReturnsDefaultQA() {
        // Arrange
        String question = "测试问题";
        String invalidResponse = "这不是有效的 JSON";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(invalidResponse);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert - 解析失败返回默认 QA
        assertEquals(UserIntent.QA, result.getIntent());
        assertEquals(0.5, result.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("classifyIntent - LLM 返回带额外文本的 JSON 时正确解析")
    void classifyIntent_JsonWithExtraText_ParsesCorrectly() {
        // Arrange
        String question = "总结";
        String responseWithExtraText = "这是分析结果：{\"intent\": \"SUMMARIZE\", \"confidence\": 0.95} 以上是结果。";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(responseWithExtraText);

        // Act
        IntentResult result = service.classifyIntent(question);

        // Assert
        assertEquals(UserIntent.SUMMARIZE, result.getIntent());
        assertEquals(0.95, result.getConfidence(), 0.01);
    }

    @Test
    @DisplayName("classifyIntentWithCache - 相同问题返回缓存结果")
    void classifyIntentWithCache_SameQuestion_ReturnsCachedResult() {
        // Arrange
        String question = "总结一下";
        String llmResponse = "{\"intent\": \"SUMMARIZE\", \"confidence\": 0.95}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(llmResponse);

        // Act - 第一次调用
        IntentResult result1 = service.classifyIntentWithCache(question);
        // Act - 第二次调用相同问题
        IntentResult result2 = service.classifyIntentWithCache(question);

        // Assert - 结果相同
        assertEquals(result1.getIntent(), result2.getIntent());
        assertEquals(result1.getConfidence(), result2.getConfidence());
        // LLM 只被调用一次（缓存生效）
        verify(mockChatClient, times(1)).prompt();
    }

    @Test
    @DisplayName("classifyIntentWithCache - 不同问题分别调用 LLM")
    void classifyIntentWithCache_DifferentQuestions_CallsLlmForEach() {
        // Arrange
        String question1 = "总结一下";
        String question2 = "什么是 RAG？";
        String response1 = "{\"intent\": \"SUMMARIZE\", \"confidence\": 0.95}";
        String response2 = "{\"intent\": \"QA\", \"confidence\": 0.88}";

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(response1, response2);

        // Act
        service.classifyIntentWithCache(question1);
        service.classifyIntentWithCache(question2);

        // Assert - LLM 被调用两次
        verify(mockChatClient, times(2)).prompt();
    }
}
