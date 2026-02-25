package com.example.videoagent.dto;

/**
 * 问答请求参数
 */
public class ChatRequest {

    private String subtitleContent;  // 新增：字幕内容
    private String question;

    public ChatRequest() {}

    public ChatRequest(String subtitleContent, String question) {
        this.subtitleContent = subtitleContent;
        this.question = question;
    }

    public String getSubtitleContent() {
        return subtitleContent;
    }

    public void setSubtitleContent(String subtitleContent) {
        this.subtitleContent = subtitleContent;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
