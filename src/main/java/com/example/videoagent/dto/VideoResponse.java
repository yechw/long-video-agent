package com.example.videoagent.dto;

/**
 * 统一响应对象
 */
public class VideoResponse {

    private boolean success;
    private String message;
    private String content;
    private String fileName;
    private int charCount;
    private Object data;

    public VideoResponse() {}

    public static VideoResponse success(String content) {
        VideoResponse response = new VideoResponse();
        response.setSuccess(true);
        response.setContent(content);
        return response;
    }

    public static VideoResponse success(String message, String content) {
        VideoResponse response = success(content);
        response.setMessage(message);
        return response;
    }

    public static VideoResponse successWithData(Object data) {
        VideoResponse response = new VideoResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static VideoResponse error(String message) {
        VideoResponse response = new VideoResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static VideoResponse uploadSuccess(String fileName, int charCount) {
        VideoResponse response = new VideoResponse();
        response.setSuccess(true);
        response.setFileName(fileName);
        response.setCharCount(charCount);
        response.setMessage("字幕上传成功");
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getCharCount() {
        return charCount;
    }

    public void setCharCount(int charCount) {
        this.charCount = charCount;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
