package com.example.videoagent.controller;

import com.example.videoagent.dto.ChatRequest;
import com.example.videoagent.dto.Concept;
import com.example.videoagent.dto.IntentResult;
import com.example.videoagent.dto.VideoResponse;
import com.example.videoagent.service.IntentClassificationService;
import com.example.videoagent.service.VideoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;
    private final IntentClassificationService intentClassificationService;

    public VideoController(VideoService videoService,
                          IntentClassificationService intentClassificationService) {
        this.videoService = videoService;
        this.intentClassificationService = intentClassificationService;
    }

    @GetMapping
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadSubtitle(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "useSample", required = false) Boolean useSample,
            Model model) throws IOException {

        String content;
        String fileName;

        if (Boolean.TRUE.equals(useSample)) {
            content = loadSampleSubtitle();
            fileName = "sample.srt (示例)";
        } else {
            if (file.isEmpty()) {
                model.addAttribute("error", "请选择文件或使用示例字幕");
                return "index";
            }
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
            fileName = file.getOriginalFilename();
        }

        model.addAttribute("subtitleLoaded", true);
        model.addAttribute("fileName", fileName);
        model.addAttribute("charCount", content.length());
        model.addAttribute("subtitleContent", content);

        return "index";
    }

    @PostMapping("/summarize")
    public String summarize(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam(value = "promptVersion", required = false) String promptVersion,
            Model model) {

        try {
            String summary = videoService.summarize(subtitleContent, promptVersion);

            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
            model.addAttribute("summary", summary);
        } catch (Exception e) {
            model.addAttribute("error", "生成总结失败: " + e.getMessage());
            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
        }

        return "index";
    }

    @PostMapping("/chat")
    public String chat(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question,
            @RequestParam(value = "promptVersion", required = false) String promptVersion,
            Model model) {

        try {
            String answer = videoService.chat(subtitleContent, question, promptVersion);

            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
            model.addAttribute("question", question);
            model.addAttribute("answer", answer);
        } catch (Exception e) {
            model.addAttribute("error", "问答失败: " + e.getMessage());
            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
        }

        return "index";
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
     * 提取知识点
     */
    @PostMapping("/extract")
    public String extractConcepts(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam(value = "promptVersion", required = false) String promptVersion,
            Model model) {

        try {
            String jsonResponse = videoService.extractConcepts(subtitleContent, promptVersion);

            // 解析 JSON 为 List<Concept>
            ObjectMapper mapper = new ObjectMapper();
            String jsonArray = extractJsonArray(jsonResponse);
            List<Concept> concepts = mapper.readValue(jsonArray,
                    new TypeReference<List<Concept>>(){});

            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
            model.addAttribute("concepts", concepts);
        } catch (Exception e) {
            model.addAttribute("error", "提取知识点失败: " + e.getMessage());
            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
        }

        return "index";
    }

    /**
     * 从 AI 响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "[]";
    }

    /**
     * 智能问答入口
     * 自动识别意图并路由到专用 Prompt
     */
    @PostMapping("/ask")
    public String smartAsk(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question,
            @RequestParam(value = "debug", required = false, defaultValue = "false") Boolean debug,
            @RequestParam(value = "promptVersion", required = false) String promptVersion,
            Model model) {

        try {
            // 执行智能问答
            String answer = videoService.smartAsk(subtitleContent, question, promptVersion);

            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
            model.addAttribute("smartQuestion", question);
            model.addAttribute("smartAnswer", answer);

            // 调试模式：返回意图信息
            if (Boolean.TRUE.equals(debug)) {
                IntentResult intentResult = intentClassificationService.classifyIntentWithCache(question);
                model.addAttribute("debugIntent", intentResult.getIntent().name());
                model.addAttribute("debugConfidence", intentResult.getConfidence());
            }
        } catch (Exception e) {
            model.addAttribute("error", "智能问答失败: " + e.getMessage());
            model.addAttribute("subtitleLoaded", true);
            model.addAttribute("subtitleContent", subtitleContent);
        }

        return "index";
    }

    /**
     * 流式智能问答入口
     * 使用 SSE (Server-Sent Events) 实现流式输出
     */
    @GetMapping(value = "/stream/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter smartAskStream(
            @RequestParam("subtitleContent") String subtitleContent,
            @RequestParam("question") String question,
            @RequestParam(value = "promptVersion", required = false) String promptVersion) {

        SseEmitter emitter = new SseEmitter(60_000L); // 60秒超时

        // 超时处理
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout");
            emitter.complete();
        });

        // 异常处理
        emitter.onError(e -> log.error("SSE error", e));

        // 订阅 Flux 流并推送到 SseEmitter
        videoService.smartAskStream(subtitleContent, question, promptVersion)
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
