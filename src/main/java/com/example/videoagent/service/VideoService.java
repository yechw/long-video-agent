package com.example.videoagent.service;

/**
 * 视频分析服务接口
 */
public interface VideoService {

    /**
     * 生成视频总结
     * @param subtitleContent 字幕内容
     * @return 总结文本
     */
    String summarize(String subtitleContent);

    /**
     * 问答对话
     * @param subtitleContent 字幕内容
     * @param question 用户问题
     * @return AI 回答
     */
    String chat(String subtitleContent, String question);
}
