package com.example.videoagent.dto;

import com.example.videoagent.enums.UserIntent;

/**
 * 意图分类结果
 */
public class IntentResult {

    private UserIntent intent;
    private Double confidence;

    public IntentResult() {}

    public IntentResult(UserIntent intent, Double confidence) {
        this.intent = intent;
        this.confidence = confidence;
    }

    public UserIntent getIntent() {
        return intent;
    }

    public void setIntent(UserIntent intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
