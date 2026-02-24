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

    // 端点将在后续任务中添加
}
