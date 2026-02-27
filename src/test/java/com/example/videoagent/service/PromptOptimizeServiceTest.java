package com.example.videoagent.service;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.example.videoagent.dto.PromptOptimizeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PromptOptimizeServiceTest {

    @Autowired
    private PromptOptimizeService promptOptimizeService;

    @Test
    void testOptimizeWithClearerGoal() {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("请总结这个视频");
        request.setOptimizationGoal("CLEARER");

        PromptOptimizeResponse response = promptOptimizeService.optimize(request);

        assertNotNull(response);
        assertNotNull(response.getOptimizedPrompt());
        assertNotNull(response.getImprovements());
    }

    @Test
    void testOptimizeWithCustomGoal() {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("test");
        request.setOptimizationGoal("CUSTOM");
        request.setCustomGoal("添加 JSON 格式");

        assertDoesNotThrow(() -> promptOptimizeService.optimize(request));
    }
}
