package com.example.videoagent.e2e;

import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for Vue SPA API Endpoints
 * These tests verify the REST API that the Vue frontend uses
 */
@SpringBootTest
@AutoConfigureMockMvc
class StreamAskE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";

    // ==================== Upload API Tests ====================

    @Nested
    @DisplayName("Upload API")
    class UploadApiTests {

        @Test
        @DisplayName("Upload content API should return sample subtitle")
        void uploadContentApi_ShouldReturnSampleSubtitle() throws Exception {
            mockMvc.perform(post("/api/upload/content")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").exists());
        }
    }

    // ==================== Summarize API Tests ====================

    @Nested
    @DisplayName("Summarize API")
    class SummarizeApiTests {

        @Test
        @DisplayName("Summarize API should work")
        void summarizeApi_ShouldWork() throws Exception {
            when(videoService.summarize(any())).thenReturn("测试摘要");

            mockMvc.perform(post("/api/summarize")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value("测试摘要"));
        }
    }

    // ==================== Chat API Tests ====================

    @Nested
    @DisplayName("Chat API")
    class ChatApiTests {

        @Test
        @DisplayName("Chat API should work")
        void chatApi_ShouldWork() throws Exception {
            when(videoService.chat(any(), any())).thenReturn("测试回答");

            mockMvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"question\":\"test question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value("测试回答"));
        }
    }

    // ==================== Smart Ask API Tests ====================

    @Nested
    @DisplayName("Smart Ask API")
    class SmartAskApiTests {

        @Test
        @DisplayName("Smart ask API should work")
        void smartAskApi_ShouldWork() throws Exception {
            when(videoService.smartAsk(any(), any())).thenReturn("智能回答");

            mockMvc.perform(post("/api/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"question\":\"test question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("智能回答"));
        }
    }

    // ==================== SSE Endpoint Tests ====================

    @Nested
    @DisplayName("SSE Endpoint")
    class SseEndpointTests {

        @Test
        @DisplayName("SSE endpoint should return correct content type")
        void sseEndpoint_ShouldReturnCorrectContentType() throws Exception {
            when(videoService.smartAskStream(any(), any()))
                    .thenReturn(Flux.just("测试"));

            mockMvc.perform(get("/api/stream/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题")
                    .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
        }

        @Test
        @DisplayName("SSE endpoint should handle URL encoded parameters")
        void sseEndpoint_ShouldHandleUrlEncodedParams() throws Exception {
            when(videoService.smartAskStream(any(), any()))
                    .thenReturn(Flux.just("回答"));

            mockMvc.perform(get("/api/stream/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "什么是 RAG？")
                    .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
        }
    }

    // ==================== Extract API Tests ====================

    @Nested
    @DisplayName("Extract API")
    class ExtractApiTests {

        @Test
        @DisplayName("API should support concept extraction")
        void api_ShouldSupportConceptExtraction() throws Exception {
            when(videoService.extractConcepts(any())).thenReturn("[{\"concept\":\"测试概念\"}]");

            mockMvc.perform(post("/api/extract")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("API should support quote extraction")
        void api_ShouldSupportQuoteExtraction() throws Exception {
            when(videoService.extractQuotes(any())).thenReturn("[{\"quote\":\"测试金句\"}]");

            mockMvc.perform(post("/api/quotes")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ==================== Search API Tests ====================

    @Nested
    @DisplayName("Search API")
    class SearchApiTests {

        @Test
        @DisplayName("API should support keyword search")
        void api_ShouldSupportKeywordSearch() throws Exception {
            when(videoService.searchKeyword(any(), any())).thenReturn("[{\"timestamp\":\"00:00:05\"}]");

            mockMvc.perform(post("/api/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"keyword\":\"关键词\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }
}
