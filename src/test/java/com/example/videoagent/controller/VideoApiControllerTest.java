package com.example.videoagent.controller;

import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.SmartAskResponse;
import com.example.videoagent.dto.VideoResponse;
import com.example.videoagent.enums.UserIntent;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.PromptOptimizeService;
import com.example.videoagent.service.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoApiController.class)
class VideoApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoService videoService;

    @MockBean
    private IntentClassificationService intentClassificationService;

    @MockBean
    private PromptOptimizeService promptOptimizeService;

    @Test
    void upload_withFile_shouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.srt", "text/plain", "test content".getBytes());

        mockMvc.perform(multipart("/api/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.fileName").value("test.srt"));
    }

    @Test
    void upload_content_shouldReturnSample() throws Exception {
        mockMvc.perform(post("/api/upload/content")
                .contentType(MediaType.TEXT_PLAIN)
                .content(""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void summarize_shouldReturnSummary() throws Exception {
        when(videoService.summarize(anyString())).thenReturn("Test summary");

        mockMvc.perform(post("/api/summarize")
                .contentType(MediaType.TEXT_PLAIN)
                .content("subtitle content"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.content").value("Test summary"));
    }

    @Test
    void ask_shouldReturnAnswer() throws Exception {
        when(videoService.smartAsk(anyString(), anyString())).thenReturn("Test answer");
        when(intentClassificationService.classifyIntentWithCache(anyString()))
            .thenReturn(new IntentResult(UserIntent.SUMMARIZE, 0.9));

        String requestBody = objectMapper.writeValueAsString(
            new com.example.videoagent.dto.ChatRequest("subtitle", "question"));

        mockMvc.perform(post("/api/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Test answer"));
    }

    @Test
    void ask_withDebug_shouldReturnIntentInfo() throws Exception {
        when(videoService.smartAsk(anyString(), anyString())).thenReturn("Test answer");
        when(intentClassificationService.classifyIntentWithCache(anyString()))
            .thenReturn(new IntentResult(UserIntent.EXTRACT_CONCEPTS, 0.85));

        String requestBody = objectMapper.writeValueAsString(
            new com.example.videoagent.dto.ChatRequest("subtitle", "提取知识点"));

        mockMvc.perform(post("/api/ask?debug=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Test answer"))
            .andExpect(jsonPath("$.intent").value("EXTRACT_CONCEPTS"))
            .andExpect(jsonPath("$.confidence").value(0.85));
    }
}
