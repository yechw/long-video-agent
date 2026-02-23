package com.example.videoagent.service;

import com.example.videoagent.config.PromptVersionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptVersionConfig versionConfig;

    private PromptTemplateService service;

    @BeforeEach
    void setUp() {
        service = new PromptTemplateService(versionConfig);
    }

    @Test
    void shouldRenderPromptWithDefaultVersion() {
        // Given
        when(versionConfig.getDefaultVersion("summarize")).thenReturn("v1");

        // When - 需要先创建 .st 文件
        String result = service.render("summarize", null, Map.of("subtitle", "测试字幕内容"));

        // Then
        assertThat(result).contains("测试字幕内容");
    }

    @Test
    void shouldRenderPromptWithSpecifiedVersion() {
        // Given - 指定版本时不调用 getDefaultVersion
        // When
        String result = service.render("chat", "v2", Map.of("subtitle", "字幕", "question", "问题"));

        // Then
        assertThat(result).contains("字幕");
        assertThat(result).contains("问题");
    }

    @Test
    void shouldThrowExceptionWhenTemplateNotFound() {
        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> service.render("nonexistent", null, Map.of())
        );
    }
}
