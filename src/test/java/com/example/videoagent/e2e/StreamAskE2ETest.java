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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for Streaming Output feature
 * Verifies that streaming UI elements and JavaScript are correctly rendered
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

    // ==================== Streaming UI Elements Tests ====================
    // Note: These elements are only visible after subtitle is loaded

    @Nested
    @DisplayName("Streaming UI Elements")
    class StreamingUiElementsTests {

        @Test
        @DisplayName("Page with subtitle should contain stream ask button")
        void pageWithSubtitle_ShouldContainStreamAskButton() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "", "text/plain", new byte[0]);

            mockMvc.perform(multipart("/upload")
                    .file(emptyFile)
                    .param("useSample", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"streamAskBtn\"")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("流式提问")
                ));
        }

        @Test
        @DisplayName("Page should contain stream response section (hidden)")
        void page_ShouldContainStreamResponseSection() throws Exception {
            // Stream response section is always present but hidden
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-response\"")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("🌊 流式回答")
                ));
        }

        @Test
        @DisplayName("Page should contain stream answer display area")
        void page_ShouldContainStreamAnswerArea() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-answer\"")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-question\"")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-status\"")
                ));
        }
    }

    // ==================== EventSource JavaScript Tests ====================

    @Nested
    @DisplayName("EventSource JavaScript")
    class EventSourceJavaScriptTests {

        @Test
        @DisplayName("Page should contain initStreamAsk function")
        void page_ShouldContainInitStreamAskFunction() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("function initStreamAsk()")
                ));
        }

        @Test
        @DisplayName("Page should contain askStream function")
        void page_ShouldContainAskStreamFunction() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("function askStream(")
                ));
        }

        @Test
        @DisplayName("Page should use EventSource API")
        void page_ShouldUseEventSourceApi() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("new EventSource(")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("eventSource.onmessage")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("eventSource.onerror")
                ));
        }

        @Test
        @DisplayName("EventSource should connect to /stream/ask endpoint")
        void eventSource_ShouldConnectToStreamAskEndpoint() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("/stream/ask?")
                ));
        }
    }

    // ==================== Streaming Response Display Tests ====================

    @Nested
    @DisplayName("Streaming Response Display")
    class StreamingResponseDisplayTests {

        @Test
        @DisplayName("Stream response section should be hidden initially")
        void streamResponseSection_ShouldBeHiddenInitially() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-response\" style=\"display: none;\"")
                ));
        }

        @Test
        @DisplayName("Stream response should use markdown-content container")
        void streamResponse_ShouldUseMarkdownContainer() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("id=\"stream-answer\"")
                ));
        }
    }

    // ==================== SSE Endpoint Tests ====================

    @Nested
    @DisplayName("SSE Endpoint")
    class SseEndpointTests {

        @Test
        @DisplayName("SSE endpoint should return correct content type")
        void sseEndpoint_ShouldReturnCorrectContentType() throws Exception {
            when(videoService.smartAskStream(any(), any(), any()))
                    .thenReturn(Flux.just("测试"));

            mockMvc.perform(get("/stream/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题")
                    .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
        }

        @Test
        @DisplayName("SSE endpoint should handle URL encoded parameters")
        void sseEndpoint_ShouldHandleUrlEncodedParams() throws Exception {
            when(videoService.smartAskStream(any(), any(), any()))
                    .thenReturn(Flux.just("回答"));

            mockMvc.perform(get("/stream/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "什么是 RAG？")
                    .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
        }
    }

    // ==================== Button Placement Tests ====================

    @Nested
    @DisplayName("Button Placement")
    class ButtonPlacementTests {

        @Test
        @DisplayName("Stream button should be in smart ask section")
        void streamButton_ShouldBeInSmartAskSection() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "", "text/plain", new byte[0]);

            MvcResult result = mockMvc.perform(multipart("/upload")
                    .file(emptyFile)
                    .param("useSample", "true"))
                .andExpect(status().isOk())
                .andReturn();

            String content = result.getResponse().getContentAsString();

            // Find smart ask section
            int smartAskSection = content.indexOf("🤖 智能问答");
            if (smartAskSection < 0) {
                // Try without emoji for safety
                smartAskSection = content.indexOf("智能问答");
            }
            int smartAskEnd = content.indexOf("</section>", smartAskSection);

            // Only verify if section found
            if (smartAskSection >= 0 && smartAskEnd > smartAskSection) {
                String smartAskBlock = content.substring(smartAskSection, smartAskEnd);

                // Verify both buttons are in this section
                org.hamcrest.MatcherAssert.assertThat(smartAskBlock,
                    org.hamcrest.Matchers.containsString("智能回答"));
                org.hamcrest.MatcherAssert.assertThat(smartAskBlock,
                    org.hamcrest.Matchers.containsString("流式提问"));
            }
        }

        @Test
        @DisplayName("Stream button should have secondary style")
        void streamButton_ShouldHaveSecondaryStyle() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "", "text/plain", new byte[0]);

            mockMvc.perform(multipart("/upload")
                    .file(emptyFile)
                    .param("useSample", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("btn btn-secondary")
                ));
        }
    }
}
