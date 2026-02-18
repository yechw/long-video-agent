package com.example.videoagent.dto;

/**
 * 智能问答响应
 */
public class SmartAskResponse {

    private String intent;      // 调试模式时返回
    private Double confidence;  // 调试模式时返回
    private String content;     // 最终结果

    public SmartAskResponse() {}

    public SmartAskResponse(String content) {
        this.content = content;
    }

    public SmartAskResponse(String intent, Double confidence, String content) {
        this.intent = intent;
        this.confidence = confidence;
        this.content = content;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
