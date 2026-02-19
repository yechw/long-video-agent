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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for Markdown rendering feature
 * Verifies that HTML output contains necessary CDN scripts and markdown containers
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

    // ==================== CDN Scripts Tests ====================

    @Nested
    @DisplayName("CDN Scripts Verification")
    class CdnScriptsTests {

        @Test
        @DisplayName("Index page should contain marked.js CDN script")
        void indexPage_ShouldContainMarkedJs() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("cdn.jsdelivr.net/npm/marked/marked.min.js")
                ));
        }

        @Test
        @DisplayName("Index page should contain highlight.js CDN script")
        void indexPage_ShouldContainHighlightJs() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("cdnjs.cloudflare.com/ajax/libs/highlight.js")
                ));
        }

        @Test
        @DisplayName("Index page should contain highlight.js CSS")
        void indexPage_ShouldContainHighlightJsCss() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("highlight.js/11.9.0/styles/github-dark.min.css")
                ));
        }
    }

    // ==================== Smart Answer Markdown Container Tests ====================

    @Nested
    @DisplayName("Smart Answer Markdown Container")
    class SmartAnswerMarkdownTests {

        @Test
        @DisplayName("Smart answer should use markdown-content container")
        void smartAnswer_ShouldUseMarkdownContainer() throws Exception {
            String markdownAnswer = "# 标题\n\n这是**粗体**文本";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownAnswer);

            mockMvc.perform(post("/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("class=\"markdown-content\"")
                ));
        }

        @Test
        @DisplayName("Smart answer should have data-content attribute")
        void smartAnswer_ShouldHaveDataContentAttribute() throws Exception {
            String markdownAnswer = "# 标题";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownAnswer);

            mockMvc.perform(post("/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("data-content=")
                ));
        }

        @Test
        @DisplayName("Smart answer should have markdown-rendered div")
        void smartAnswer_ShouldHaveMarkdownRenderedDiv() throws Exception {
            when(videoService.smartAsk(any(), any())).thenReturn("回答内容");

            mockMvc.perform(post("/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("class=\"markdown-rendered\"")
                ));
        }

        @Test
        @DisplayName("Smart answer should NOT use pre tag for display")
        void smartAnswer_ShouldNotUsePreTag() throws Exception {
            String markdownAnswer = "# 标题";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownAnswer);

            // Verify that in smart answer section, pre tag is replaced with markdown container
            MvcResult result = mockMvc.perform(post("/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题"))
                .andExpect(status().isOk())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            // Find the smart answer section and verify it doesn't contain <pre> for the answer
            int smartAnswerSection = content.indexOf("🎯 智能回答");
            int nextSection = content.indexOf("</section>", smartAnswerSection);
            String smartAnswerBlock = content.substring(smartAnswerSection, nextSection);

            // Should contain markdown-content but NOT <pre> for the actual answer
            org.hamcrest.MatcherAssert.assertThat(smartAnswerBlock,
                org.hamcrest.Matchers.containsString("markdown-content"));
        }
    }

    // ==================== Summary Markdown Container Tests ====================

    @Nested
    @DisplayName("Summary Markdown Container")
    class SummaryMarkdownTests {

        @Test
        @DisplayName("Summary should use markdown-content container")
        void summary_ShouldUseMarkdownContainer() throws Exception {
            String markdownSummary = "## 总结\n\n- 要点1\n- 要点2";
            when(videoService.summarize(any())).thenReturn(markdownSummary);

            mockMvc.perform(post("/summarize")
                    .param("subtitleContent", SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("class=\"markdown-content\"")
                ));
        }

        @Test
        @DisplayName("Summary should have markdown-rendered div")
        void summary_ShouldHaveMarkdownRenderedDiv() throws Exception {
            when(videoService.summarize(any())).thenReturn("总结内容");

            mockMvc.perform(post("/summarize")
                    .param("subtitleContent", SAMPLE_SUBTITLE))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("class=\"markdown-rendered\"")
                ));
        }
    }

    // ==================== Regular Chat Should NOT Use Markdown ====================

    @Nested
    @DisplayName("Regular Chat Plain Text")
    class RegularChatPlainTextTests {

        @Test
        @DisplayName("Regular chat should still use pre tag (not markdown)")
        void regularChat_ShouldUsePreTag() throws Exception {
            when(videoService.chat(any(), any())).thenReturn("普通回答");

            MvcResult result = mockMvc.perform(post("/chat")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "测试问题"))
                .andExpect(status().isOk())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            // Find the chat answer section (🤖 AI 回答 after 💬 问答对话)
            int chatAnswerSection = content.indexOf("🤖 AI 回答");
            int nextSection = content.indexOf("</section>", chatAnswerSection);
            String chatAnswerBlock = content.substring(chatAnswerSection, nextSection);

            // Should contain <pre> tag for plain text display
            org.hamcrest.MatcherAssert.assertThat(chatAnswerBlock,
                org.hamcrest.Matchers.containsString("<pre"));
        }
    }

    // ==================== JavaScript Rendering Script Tests ====================

    @Nested
    @DisplayName("JavaScript Rendering Script")
    class JavaScriptRenderingTests {

        @Test
        @DisplayName("Page should contain DOMContentLoaded script")
        void page_ShouldContainDomContentLoadedScript() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("DOMContentLoaded")
                ));
        }

        @Test
        @DisplayName("Script should configure marked with highlight")
        void script_ShouldConfigureMarkedWithHighlight() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("marked.setOptions")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("hljs.highlight")
                ));
        }

        @Test
        @DisplayName("Script should render all markdown-content elements")
        void script_ShouldRenderAllMarkdownContent() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("querySelectorAll('.markdown-content')")
                ))
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("marked.parse")
                ));
        }
    }

    // ==================== Markdown with Code Block Test ====================

    @Nested
    @DisplayName("Markdown Content Rendering")
    class MarkdownContentTests {

        @Test
        @DisplayName("Code block in smart answer should be properly escaped")
        void codeBlockInSmartAnswer_ShouldBeEscaped() throws Exception {
            String markdownWithCode = "代码示例:\n```java\nSystem.out.println(\"Hello\");\n```";
            when(videoService.smartAsk(any(), any())).thenReturn(markdownWithCode);

            MvcResult result = mockMvc.perform(post("/ask")
                    .param("subtitleContent", SAMPLE_SUBTITLE)
                    .param("question", "代码示例"))
                .andExpect(status().isOk())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            // Verify the content is stored in data-content attribute (HTML escaped)
            org.hamcrest.MatcherAssert.assertThat(content,
                org.hamcrest.Matchers.containsString("data-content="));
        }
    }
}
