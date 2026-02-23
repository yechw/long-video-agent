package com.example.videoagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PromptVersionConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                "prompts.summarize.default-version=v1",
                "prompts.summarize.versions.v1.description=原始版本，Few-Shot 示例",
                "prompts.summarize.versions.v1.created=2026-02-17",
                "prompts.summarize.versions.v2.description=优化版本，待测试",
                "prompts.summarize.versions.v2.created=2026-02-22",
                "prompts.chat.default-version=v1",
                "prompts.chat.versions.v1.description=问答模板，支持时间戳引用",
                "prompts.chat.versions.v1.created=2026-02-17"
            );

    @Configuration
    @EnableConfigurationProperties(PromptVersionConfig.class)
    static class TestConfig {
    }

    @Test
    void shouldLoadPromptVersionsConfig() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            assertThat(config).isNotNull();
            assertThat(config.getPrompts()).containsKey("summarize");
        });
    }

    @Test
    void shouldGetDefaultVersion() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            PromptDefinition summarize = config.getPrompts().get("summarize");
            assertThat(summarize).isNotNull();
            assertThat(summarize.getDefaultVersion()).isEqualTo("v1");
        });
    }

    @Test
    void shouldGetVersionMetadata() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            PromptDefinition summarize = config.getPrompts().get("summarize");
            assertThat(summarize).isNotNull();
            VersionInfo v1 = summarize.getVersions().get("v1");
            assertThat(v1).isNotNull();
            assertThat(v1.getDescription()).isEqualTo("原始版本，Few-Shot 示例");
        });
    }

    @Test
    void shouldGetDefaultVersionFromHelperMethod() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            assertThat(config.getDefaultVersion("summarize")).isEqualTo("v1");
            assertThat(config.getDefaultVersion("chat")).isEqualTo("v1");
            assertThat(config.getDefaultVersion("nonexistent")).isEqualTo("v1");
        });
    }

    @Test
    void shouldCheckVersionExists() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            assertThat(config.hasVersion("summarize", "v1")).isTrue();
            assertThat(config.hasVersion("summarize", "v2")).isTrue();
            assertThat(config.hasVersion("summarize", "v3")).isFalse();
            assertThat(config.hasVersion("nonexistent", "v1")).isFalse();
        });
    }
}
