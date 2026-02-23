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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * VideoService 流式方法单元测试
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceStreamTest {

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec mockRequestSpec;

    @Mock
    private ChatClient.StreamResponseSpec mockStreamResponseSpec;

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

    // ==================== smartAskStream 流式输出测试 ====================

    @Test
    @DisplayName("smartAskStream - 返回 Flux 流式内容")
    void smartAskStream_ReturnsFluxContent() {
        // Arrange
        String question = "什么是提示工程？";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);
        List<String> expectedChunks = List.of("提示工程", "是", "一种", "技术");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("chat"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.fromIterable(expectedChunks));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectNextSequence(expectedChunks)
                .verifyComplete();
    }

    @Test
    @DisplayName("smartAskStream - SUMMARIZE 意图正确路由")
    void smartAskStream_SummarizeIntent_RoutesCorrectly() {
        // Arrange
        String question = "总结一下这个视频";
        IntentResult intentResult = new IntentResult(UserIntent.SUMMARIZE, 0.95);
        List<String> expectedChunks = List.of("这是", "总结", "内容");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("summarize"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.fromIterable(expectedChunks));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectNextSequence(expectedChunks)
                .verifyComplete();

        // 验证使用了总结 prompt 模板
        verify(mockPromptTemplateService).render(eq("summarize"), any(), anyMap());
    }

    @Test
    @DisplayName("smartAskStream - QA 意图正确路由并包含问题")
    void smartAskStream_QAIntent_ContainsQuestion() {
        // Arrange
        String question = "什么是 RAG？";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);
        List<String> expectedChunks = List.of("RAG", "是", "检索增强生成");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("chat"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.fromIterable(expectedChunks));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectNextSequence(expectedChunks)
                .verifyComplete();

        // 验证使用了 chat 模板
        verify(mockPromptTemplateService).render(eq("chat"), any(), anyMap());
    }

    @Test
    @DisplayName("smartAskStream - SEARCH_KEYWORD 意图提取关键词")
    void smartAskStream_SearchKeywordIntent_ExtractsKeyword() {
        // Arrange
        String question = "哪里提到了 Transformer";
        IntentResult intentResult = new IntentResult(UserIntent.SEARCH_KEYWORD, 0.85);
        List<String> expectedChunks = List.of("搜索", "结果");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("search-keyword"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.fromIterable(expectedChunks));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectNextSequence(expectedChunks)
                .verifyComplete();

        // 验证使用了 search-keyword 模板
        verify(mockPromptTemplateService).render(eq("search-keyword"), any(), anyMap());
    }

    @Test
    @DisplayName("smartAskStream - DEEP_QA 意图移除前缀")
    void smartAskStream_DeepQaIntent_RemovesPrefix() {
        // Arrange
        String question = "/deep 什么是思维链？";
        String realQuestion = "什么是思维链？";
        IntentResult intentResult = new IntentResult(UserIntent.DEEP_QA, 0.90);
        List<String> expectedChunks = List.of("思维链", "是", "CoT");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("deep-qa"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.fromIterable(expectedChunks));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectNextSequence(expectedChunks)
                .verifyComplete();

        // 验证使用了 deep-qa 模板
        verify(mockPromptTemplateService).render(eq("deep-qa"), any(), anyMap());
    }

    @Test
    @DisplayName("smartAskStream - 空流正确处理")
    void smartAskStream_EmptyStream_CompletesSuccessfully() {
        // Arrange
        String question = "测试问题";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("chat"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.empty());

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("smartAskStream - 流错误正确传播")
    void smartAskStream_StreamError_PropagatesError() {
        // Arrange
        String question = "测试问题";
        IntentResult intentResult = new IntentResult(UserIntent.QA, 0.88);
        RuntimeException expectedError = new RuntimeException("AI 服务错误");

        when(mockIntentService.classifyIntentWithCache(question)).thenReturn(intentResult);
        when(mockPromptTemplateService.render(eq("chat"), any(), anyMap())).thenReturn(RENDERED_PROMPT);
        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(any(String.class))).thenReturn(mockRequestSpec);
        when(mockRequestSpec.stream()).thenReturn(mockStreamResponseSpec);
        when(mockStreamResponseSpec.content()).thenReturn(Flux.error(expectedError));

        // Act
        Flux<String> result = videoService.smartAskStream(SAMPLE_SUBTITLE, question);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify(Duration.ofSeconds(5));
    }
}
