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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for API endpoints that return Markdown content
 * The Vue SPA handles markdown rendering on the client side
 */
@SpringBootTest
@AutoConfigureMockMvc
class MarkdownRenderingE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";

    // ==================== API Response Tests ====================

    @Nested
    @DisplayName("API Response Format")
    class ApiResponseTests {

        @Test
        @DisplayName("Summarize API should return JSON response with content")
        void summarizeApi_ShouldReturnJsonResponse() throws Exception {
            String markdownSummary = "## 总结\n\n- 要点1\n- 要点2";
            when(videoService.summarize(any())).thenReturn(markdownSummary);

            mockMvc.perform(post("/api/summarize")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value(markdownSummary));
        }

        @Test
        @DisplayName("Smart ask API should return JSON response with markdown content")
        void smartAskApi_ShouldReturnJsonResponse() throws Exception {
            String markdownAnswer = "# 标题\n\n这是**粗体**文本";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownAnswer);

            mockMvc.perform(post("/api/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"question\":\"测试问题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(markdownAnswer));
        }

        @Test
        @DisplayName("Chat API should return JSON response")
        void chatApi_ShouldReturnJsonResponse() throws Exception {
            String answer = "普通回答";
            when(videoService.chat(any(), any())).thenReturn(answer);

            mockMvc.perform(post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"question\":\"测试问题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value(answer));
        }
    }

    // ==================== Markdown Content in API Responses ====================

    @Nested
    @DisplayName("Markdown Content Handling")
    class MarkdownContentTests {

        @Test
        @DisplayName("API should preserve markdown formatting in responses")
        void api_ShouldPreserveMarkdownFormatting() throws Exception {
            String markdownWithFormatting = "# Heading\n\n**Bold** and *italic*\n\n- List item 1\n- List item 2";
            when(videoService.summarize(any())).thenReturn(markdownWithFormatting);

            mockMvc.perform(post("/api/summarize")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(markdownWithFormatting));
        }

        @Test
        @DisplayName("API should handle code blocks in markdown")
        void api_ShouldHandleCodeBlocks() throws Exception {
            String markdownWithCode = "代码示例:\n```java\nSystem.out.println(\"Hello\");\n```";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownWithCode);

            mockMvc.perform(post("/api/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"subtitleContent\":\"test\",\"question\":\"代码示例\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(markdownWithCode));
        }

        @Test
        @DisplayName("API should handle special characters in markdown")
        void api_ShouldHandleSpecialCharacters() throws Exception {
            String markdownWithSpecialChars = "特殊字符: <>&\"'\n\n**粗体** _斜体_";
            when(videoService.summarize(any())).thenReturn(markdownWithSpecialChars);

            mockMvc.perform(post("/api/summarize")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(markdownWithSpecialChars));
        }
    }

    // ==================== Extract API Response Tests ====================

    @Nested
    @DisplayName("Extract API Response Format")
    class ExtractApiTests {

        @Test
        @DisplayName("Extract concepts API should return JSON array in content")
        void extractConceptsApi_ShouldReturnJsonArray() throws Exception {
            String conceptsJson = "[{\"timestampFrom\":\"00:00:00\",\"timestampTo\":\"00:00:30\",\"concept\":\"提示工程\",\"description\":\"核心技能\"}]";
            when(videoService.extractConcepts(any())).thenReturn(conceptsJson);

            mockMvc.perform(post("/api/extract")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value(conceptsJson));
        }

        @Test
        @DisplayName("Extract quotes API should return JSON array in content")
        void extractQuotesApi_ShouldReturnJsonArray() throws Exception {
            String quotesJson = "[{\"timestamp\":\"00:00:05\",\"quote\":\"金句内容\"}]";
            when(videoService.extractQuotes(any())).thenReturn(quotesJson);

            mockMvc.perform(post("/api/quotes")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value(quotesJson));
        }
    }
}
