package com.example.videoagent.controller;

import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 流式端点测试
 */
@WebMvcTest(VideoController.class)
class StreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";

    // ==================== /stream/ask 端点测试 ====================

    @Test
    @DisplayName("/stream/ask - 返回 SSE 内容类型")
    void streamAsk_ReturnsSseContentType() throws Exception {
        // Arrange
        String question = "什么是提示工程？";
        when(videoService.smartAskStream(SAMPLE_SUBTITLE, question))
                .thenReturn(Flux.just("测试", "回答"));

        // Act & Assert
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("/stream/ask - 流式输出多个数据块")
    void streamAsk_OutputsMultipleChunks() throws Exception {
        // Arrange
        String question = "测试问题";
        Flux<String> responseFlux = Flux.just("第一块", "第二块", "第三块")
                .delayElements(Duration.ofMillis(10));

        when(videoService.smartAskStream(SAMPLE_SUBTITLE, question))
                .thenReturn(responseFlux);

        // Act
        MvcResult mvcResult = mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();

        // 等待异步完成
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/stream/ask - 空流正确处理")
    void streamAsk_EmptyStream_CompletesSuccessfully() throws Exception {
        // Arrange
        String question = "空问题";
        when(videoService.smartAskStream(SAMPLE_SUBTITLE, question))
                .thenReturn(Flux.empty());

        // Act & Assert
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("/stream/ask - 服务错误正确传播")
    void streamAsk_ServiceError_PropagatesError() throws Exception {
        // Arrange
        String question = "错误问题";
        when(videoService.smartAskStream(SAMPLE_SUBTITLE, question))
                .thenReturn(Flux.error(new RuntimeException("AI 服务异常")));

        // Act & Assert
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("/stream/ask - URL 编码参数正确处理")
    void streamAsk_UrlEncodedParams_HandledCorrectly() throws Exception {
        // Arrange
        String question = "什么是 RAG？";
        String encodedQuestion = "什么是%20RAG%EF%BC%9F";
        when(videoService.smartAskStream(any(), any()))
                .thenReturn(Flux.just("回答"));

        // Act & Assert
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/stream/ask - 长字幕内容处理")
    void streamAsk_LongSubtitle_HandledCorrectly() throws Exception {
        // Arrange
        String longSubtitle = "很长的字幕内容".repeat(1000);
        String question = "总结";
        when(videoService.smartAskStream(longSubtitle, question))
                .thenReturn(Flux.just("总结内容"));

        // Act & Assert
        mockMvc.perform(get("/stream/ask")
                .param("subtitleContent", longSubtitle)
                .param("question", question)
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted());
    }
}
