package com.example.videoagent.e2e;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PromptOptimizeE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testOptimizePromptEndpoint() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("请总结这个视频的内容");
        request.setOptimizationGoal("CLEARER");

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.optimizedPrompt").exists())
            .andExpect(jsonPath("$.data.improvements").isArray());
    }

    @Test
    void testOptimizePromptWithCustomGoal() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("提取视频中的知识点");
        request.setOptimizationGoal("CUSTOM");
        request.setCustomGoal("添加 JSON 格式要求");

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testOptimizePromptValidation() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        // Missing required fields

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
