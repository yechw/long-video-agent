package com.example.videoagent.dto;

/**
 * 搜索请求参数
 */
public class SearchRequest {

    private String subtitleContent;
    private String keyword;

    public SearchRequest() {}

    public SearchRequest(String subtitleContent, String keyword) {
        this.subtitleContent = subtitleContent;
        this.keyword = keyword;
    }

    public String getSubtitleContent() {
        return subtitleContent;
    }

    public void setSubtitleContent(String subtitleContent) {
        this.subtitleContent = subtitleContent;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
