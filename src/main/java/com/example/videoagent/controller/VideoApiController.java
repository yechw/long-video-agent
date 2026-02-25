package com.example.videoagent.controller;

import com.example.videoagent.dto.ChatRequest;
import com.example.videoagent.dto.Concept;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.SearchRequest;
import com.example.videoagent.dto.SmartAskResponse;
import com.example.videoagent.dto.VideoResponse;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoApiController {

    private static final Logger log = LoggerFactory.getLogger(VideoApiController.class);

    private final VideoService videoService;
    private final IntentClassificationService intentClassificationService;

    public VideoApiController(VideoService videoService,
                              IntentClassificationService intentClassificationService) {
        this.videoService = videoService;
        this.intentClassificationService = intentClassificationService;
    }

    /**
     * 上传字幕文件
     */
    @PostMapping("/upload")
    public VideoResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "useSample", required = false) Boolean useSample) throws IOException {

        String content;
        String fileName;

        if (Boolean.TRUE.equals(useSample)) {
            content = loadSampleSubtitle();
            fileName = "sample.srt (示例)";
        } else {
            if (file.isEmpty()) {
                return VideoResponse.error("请选择文件或使用示例字幕");
            }
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
            fileName = file.getOriginalFilename();
        }

        return VideoResponse.uploadSuccess(fileName, content.length());
    }

    /**
     * 获取上传的字幕内容（用于示例字幕或直接提交内容）
     */
    @PostMapping("/upload/content")
    public VideoResponse uploadWithContent(@RequestBody(required = false) String content) {
        if (content == null || content.isEmpty()) {
            content = loadSampleSubtitle();
        }
        return VideoResponse.success("字幕加载成功", content);
    }

    private String loadSampleSubtitle() {
        return """
                1
                00:00:00,000 --> 00:00:05,000
                大家好，欢迎来到今天的 AI 工程课程。

                2
                00:00:05,000 --> 00:00:12,000
                今天我们要讨论的是提示工程（Prompt Engineering），
                这是构建 AI 应用的核心技能之一。

                3
                00:00:12,000 --> 00:00:20,000
                提示工程的核心在于如何有效地与大型语言模型沟通，
                让模型生成我们期望的输出。

                4
                00:00:20,000 --> 00:00:30,000
                一个好的提示包含三个关键要素：
                任务描述、示例、以及具体的任务指令。

                5
                00:00:30,000 --> 00:00:40,000
                系统提示（System Prompt）用于设定 AI 的角色和行为边界，
                而用户提示（User Prompt）则包含具体的任务内容。

                6
                00:00:40,000 --> 00:00:50,000
                在处理长文本时，有一个重要的技巧：
                将关键指令放在提示的末尾，这样可以防止指令被长文本冲淡。

                7
                00:00:50,000 --> 00:01:00,000
                另外，为了防止模型产生幻觉，
                我们需要在系统提示中明确限制模型只能基于提供的内容回答。

                8
                00:01:00,000 --> 00:01:10,000
                Few-shot prompting 是另一个强大的技术，
                通过提供几个示例，可以让模型快速学习特定的输出格式。

                9
                00:01:10,000 --> 00:01:20,000
                总结一下，提示工程的关键点包括：
                清晰的指令、合适的示例、以及明确的约束条件。

                10
                00:01:20,000 --> 00:01:30,000
                下一节课，我们将讨论如何使用 RAG 技术
                来增强 AI 应用的知识检索能力。
                """;
    }

    /**
     * 生成视频摘要
     */
    @PostMapping("/summarize")
    public VideoResponse summarize(@RequestBody String subtitleContent) {
        try {
            String summary = videoService.summarize(subtitleContent);
            return VideoResponse.success(summary);
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return VideoResponse.error("生成摘要失败: " + e.getMessage());
        }
    }

    /**
     * 基础问答
     */
    @PostMapping("/chat")
    public VideoResponse chat(@RequestBody ChatRequest request) {
        try {
            String answer = videoService.chat(request.getSubtitleContent(), request.getQuestion());
            return VideoResponse.success(answer);
        } catch (Exception e) {
            log.error("问答失败", e);
            return VideoResponse.error("问答失败: " + e.getMessage());
        }
    }

    /**
     * 提取知识概念
     */
    @PostMapping("/extract")
    public VideoResponse extractConcepts(@RequestBody String subtitleContent) {
        try {
            String jsonResponse = videoService.extractConcepts(subtitleContent);
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("提取知识点失败", e);
            return VideoResponse.error("提取知识点失败: " + e.getMessage());
        }
    }

    /**
     * 提取金句
     */
    @PostMapping("/quotes")
    public VideoResponse extractQuotes(@RequestBody String subtitleContent) {
        try {
            String jsonResponse = videoService.extractQuotes(subtitleContent);
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("提取金句失败", e);
            return VideoResponse.error("提取金句失败: " + e.getMessage());
        }
    }

    /**
     * 关键词搜索
     */
    @PostMapping("/search")
    public VideoResponse searchKeyword(@RequestBody SearchRequest request) {
        try {
            String jsonResponse = videoService.searchKeyword(
                request.getSubtitleContent(), request.getKeyword());
            return VideoResponse.success(jsonResponse);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return VideoResponse.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 智能问答（自动意图分类）
     */
    @PostMapping("/ask")
    public SmartAskResponse smartAsk(
            @RequestBody ChatRequest request,
            @RequestParam(value = "debug", required = false, defaultValue = "false") Boolean debug) {
        try {
            String answer = videoService.smartAsk(
                request.getSubtitleContent(), request.getQuestion());

            if (Boolean.TRUE.equals(debug)) {
                IntentResult intentResult = intentClassificationService
                    .classifyIntentWithCache(request.getQuestion());
                return new SmartAskResponse(
                    intentResult.getIntent().name(),
                    intentResult.getConfidence(),
                    answer);
            }
            return new SmartAskResponse(answer);
        } catch (Exception e) {
            log.error("智能问答失败", e);
            SmartAskResponse response = new SmartAskResponse();
            response.setContent("智能问答失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 流式智能问答（SSE）- POST 版本，支持长字幕内容
     */
    @PostMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter smartAskStream(@RequestBody ChatRequest request) {

        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout");
            emitter.complete();
        });

        emitter.onError(e -> log.error("SSE error", e));

        videoService.smartAskStream(request.getSubtitleContent(), request.getQuestion())
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            })
            .doOnComplete(emitter::complete)
            .doOnError(error -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成失败: " + error.getMessage()));
                    emitter.complete();
                } catch (IOException ignored) {}
            })
            .subscribe();

        return emitter;
    }
}
