package com.example.videoagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PromptOptimizeRequest {

    @NotBlank(message = "原始 Prompt 不能为空")
    @Size(max = 4000, message = "Prompt 长度不能超过 4000 字符")
    private String originalPrompt;

    @NotBlank(message = "优化目标不能为空")
    private String optimizationGoal; // CLEARER, CONCISE, STRICT, COMPLETE, CUSTOM

    @Size(max = 500, message = "自定义目标描述不能超过 500 字符")
    private String customGoal;

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(String originalPrompt) {
        this.originalPrompt = originalPrompt;
    }

    public String getOptimizationGoal() {
        return optimizationGoal;
    }

    public void setOptimizationGoal(String optimizationGoal) {
        this.optimizationGoal = optimizationGoal;
    }

    public String getCustomGoal() {
        return customGoal;
    }

    public void setCustomGoal(String customGoal) {
        this.customGoal = customGoal;
    }
}
