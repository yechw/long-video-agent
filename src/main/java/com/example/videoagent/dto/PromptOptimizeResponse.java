package com.example.videoagent.dto;

import java.util.List;

public class PromptOptimizeResponse {

    private String optimizedPrompt;
    private List<String> improvements;

    public PromptOptimizeResponse() {}

    public PromptOptimizeResponse(String optimizedPrompt, List<String> improvements) {
        this.optimizedPrompt = optimizedPrompt;
        this.improvements = improvements;
    }

    public String getOptimizedPrompt() {
        return optimizedPrompt;
    }

    public void setOptimizedPrompt(String optimizedPrompt) {
        this.optimizedPrompt = optimizedPrompt;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }
}
