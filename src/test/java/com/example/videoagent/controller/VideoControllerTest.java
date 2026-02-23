package com.example.videoagent.controller;

import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.enums.UserIntent;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VideoController 单元测试
 */
@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    private static final String SAMPLE_SUBTITLE = "[00:00:05] 测试字幕内容";

    // ==================== /ask 端点测试 ====================

    @Test
    @DisplayName("/ask - 正常请求返回智能回答")
    void ask_NormalRequest_ReturnsSmartAnswer() throws Exception {
        // Arrange
        String question = "总结一下";
        String expectedAnswer = "这是总结内容";

        when(videoService.smartAsk(SAMPLE_SUBTITLE, question, null)).thenReturn(expectedAnswer);

        // Act & Assert
        mockMvc.perform(post("/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("smartAnswer", expectedAnswer))
            .andExpect(model().attribute("smartQuestion", question))
            .andExpect(model().attribute("subtitleLoaded", true));
    }

    @Test
    @DisplayName("/ask - 调试模式返回意图信息")
    void ask_DebugMode_ReturnsIntentInfo() throws Exception {
        // Arrange
        String question = "总结一下";
        String expectedAnswer = "这是总结内容";
        IntentResult intentResult = new IntentResult(UserIntent.SUMMARIZE, 0.95);

        when(videoService.smartAsk(SAMPLE_SUBTITLE, question, null)).thenReturn(expectedAnswer);
        when(intentClassificationService.classifyIntentWithCache(question)).thenReturn(intentResult);

        // Act & Assert
        mockMvc.perform(post("/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .param("debug", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("smartAnswer", expectedAnswer))
            .andExpect(model().attribute("debugIntent", "SUMMARIZE"))
            .andExpect(model().attribute("debugConfidence", 0.95));
    }

    @Test
    @DisplayName("/ask - 调试模式关闭时不返回意图信息")
    void ask_DebugModeOff_NoIntentInfo() throws Exception {
        // Arrange
        String question = "总结一下";
        String expectedAnswer = "这是总结内容";

        when(videoService.smartAsk(SAMPLE_SUBTITLE, question, null)).thenReturn(expectedAnswer);

        // Act & Assert
        mockMvc.perform(post("/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question)
                .param("debug", "false"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("smartAnswer", expectedAnswer))
            .andExpect(model().attributeDoesNotExist("debugIntent"))
            .andExpect(model().attributeDoesNotExist("debugConfidence"));
    }

    @Test
    @DisplayName("/ask - 服务异常返回错误信息")
    void ask_ServiceException_ReturnsError() throws Exception {
        // Arrange
        String question = "总结一下";
        when(videoService.smartAsk(SAMPLE_SUBTITLE, question, null))
            .thenThrow(new RuntimeException("服务异常"));

        // Act & Assert
        mockMvc.perform(post("/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("error", org.hamcrest.Matchers.containsString("智能问答失败")))
            .andExpect(model().attribute("subtitleLoaded", true));
    }

    @Test
    @DisplayName("/ask - 保留字幕内容")
    void ask_PreservesSubtitleContent() throws Exception {
        // Arrange
        String question = "总结一下";
        when(videoService.smartAsk(SAMPLE_SUBTITLE, question, null)).thenReturn("回答");

        // Act & Assert
        mockMvc.perform(post("/ask")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question))
            .andExpect(status().isOk())
            .andExpect(model().attribute("subtitleContent", SAMPLE_SUBTITLE));
    }

    // ==================== 其他端点回归测试 ====================

    @Test
    @DisplayName("/summarize - 正常请求返回总结")
    void summarize_NormalRequest_ReturnsSummary() throws Exception {
        // Arrange
        String expectedSummary = "这是总结";
        when(videoService.summarize(SAMPLE_SUBTITLE, null)).thenReturn(expectedSummary);

        // Act & Assert
        mockMvc.perform(post("/summarize")
                .param("subtitleContent", SAMPLE_SUBTITLE))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("summary", expectedSummary));
    }

    @Test
    @DisplayName("/chat - 正常请求返回回答")
    void chat_NormalRequest_ReturnsAnswer() throws Exception {
        // Arrange
        String question = "什么是 RAG？";
        String expectedAnswer = "RAG 是检索增强生成";
        when(videoService.chat(SAMPLE_SUBTITLE, question, null)).thenReturn(expectedAnswer);

        // Act & Assert
        mockMvc.perform(post("/chat")
                .param("subtitleContent", SAMPLE_SUBTITLE)
                .param("question", question))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attribute("answer", expectedAnswer))
            .andExpect(model().attribute("question", question));
    }

    @Test
    @DisplayName("/extract - 正常请求返回知识点")
    void extract_NormalRequest_ReturnsConcepts() throws Exception {
        // Arrange
        String jsonResponse = "[{\"timestampFrom\":\"00:00:00\",\"timestampTo\":\"00:01:00\",\"concept\":\"测试\",\"description\":\"描述\"}]";
        when(videoService.extractConcepts(SAMPLE_SUBTITLE, null)).thenReturn(jsonResponse);

        // Act & Assert
        mockMvc.perform(post("/extract")
                .param("subtitleContent", SAMPLE_SUBTITLE))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("concepts"));
    }
}
