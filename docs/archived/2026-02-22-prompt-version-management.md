# Prompt 版本管理实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 Prompt 版本管理系统，支持 .st 文件格式、版本化配置、A/B 测试和请求参数切换。

**Architecture:** 使用 Spring AI 原生的 StTemplateRenderer 渲染 .st 模板文件，通过 PromptVersionConfig 加载版本配置，PromptTemplateService 提供统一的 Prompt 渲染接口，Controller 支持可选的 promptVersion 参数。

**Tech Stack:** Spring AI, StringTemplate (ST), YAML 配置, Spring ConfigurationProperties

---

## Prerequisites

确保已安装依赖：
```xml
<!-- Spring AI 已包含 StTemplateRenderer -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
</dependency>
```

---

## Task 1: 创建 prompt-versions.yml 配置文件

**Files:**
- Create: `src/main/resources/prompts/prompt-versions.yml`

**Step 1: 创建配置文件**

```yaml
# Prompt 版本配置
prompts:
  summarize:
    default: v1
    versions:
      v1:
        description: 原始版本，Few-Shot 示例
        created: 2026-02-17
      v2:
        description: 优化版本，待测试
        created: 2026-02-22

  chat:
    default: v1
    versions:
      v1:
        description: 问答模板，支持时间戳引用
        created: 2026-02-17

  extract-concepts:
    default: v1
    versions:
      v1:
        description: 知识点提取，JSON 输出
        created: 2026-02-17

  intent-classification:
    default: v1
    versions:
      v1:
        description: 意图分类器
        created: 2026-02-17

  extract-quotes:
    default: v1
    versions:
      v1:
        description: 金句提取
        created: 2026-02-17

  search-keyword:
    default: v1
    versions:
      v1:
        description: 关键词搜索
        created: 2026-02-17

  deep-qa:
    default: v1
    versions:
      v1:
        description: 深度分析，CoT 思维链
        created: 2026-02-18
```

**Step 2: 验证文件创建**

Run: `cat src/main/resources/prompts/prompt-versions.yml`
Expected: 配置文件内容显示

**Step 3: Commit**

```bash
git add src/main/resources/prompts/prompt-versions.yml
git commit -m "feat: add prompt-versions.yml configuration"
```

---

## Task 2: TDD - 实现 PromptVersionConfig 配置类

**Files:**
- Create: `src/test/java/com/example/videoagent/config/PromptVersionConfigTest.java`
- Create: `src/main/java/com/example/videoagent/config/PromptVersionConfig.java`

**Step 1: Write the failing test**

```java
package com.example.videoagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PromptVersionConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues("spring.config.import=classpath:prompts/prompt-versions.yml");

    @EnableConfigurationProperties(PromptVersionConfig.class)
    static class TestConfig {}

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
            assertThat(summarize.getDefaultVersion()).isEqualTo("v1");
        });
    }

    @Test
    void shouldGetVersionMetadata() {
        contextRunner.run(context -> {
            PromptVersionConfig config = context.getBean(PromptVersionConfig.class);
            PromptDefinition summarize = config.getPrompts().get("summarize");
            VersionInfo v1 = summarize.getVersions().get("v1");
            assertThat(v1.getDescription()).isEqualTo("原始版本，Few-Shot 示例");
        });
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PromptVersionConfigTest -q`
Expected: FAIL with "Unable to load ConfigurationProperties" or class not found

**Step 3: Write minimal implementation - VersionInfo**

```java
package com.example.videoagent.config;

/**
 * 版本信息
 */
public class VersionInfo {
    private String description;
    private String created;
    private boolean deprecated = false;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}
```

**Step 4: Write minimal implementation - PromptDefinition**

```java
package com.example.videoagent.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 定义，包含默认版本和所有版本信息
 */
public class PromptDefinition {
    private String defaultVersion;
    private Map<String, VersionInfo> versions = new HashMap<>();

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public Map<String, VersionInfo> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, VersionInfo> versions) {
        this.versions = versions;
    }
}
```

**Step 5: Write minimal implementation - PromptVersionConfig**

```java
package com.example.videoagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 版本配置
 * 从 prompts/prompt-versions.yml 加载
 */
@Configuration
@ConfigurationProperties(prefix = "")
public class PromptVersionConfig {
    private Map<String, PromptDefinition> prompts = new HashMap<>();

    public Map<String, PromptDefinition> getPrompts() {
        return prompts;
    }

    public void setPrompts(Map<String, PromptDefinition> prompts) {
        this.prompts = prompts;
    }

    /**
     * 获取指定 Prompt 的默认版本
     */
    public String getDefaultVersion(String promptName) {
        PromptDefinition def = prompts.get(promptName);
        return def != null ? def.getDefaultVersion() : "v1";
    }

    /**
     * 检查版本是否存在
     */
    public boolean hasVersion(String promptName, String version) {
        PromptDefinition def = prompts.get(promptName);
        if (def == null) return false;
        return def.getVersions().containsKey(version);
    }
}
```

**Step 6: Run test to verify it passes**

Run: `./mvnw test -Dtest=PromptVersionConfigTest -q`
Expected: PASS (3 tests)

**Step 7: Commit**

```bash
git add src/main/java/com/example/videoagent/config/VersionInfo.java \
        src/main/java/com/example/videoagent/config/PromptDefinition.java \
        src/main/java/com/example/videoagent/config/PromptVersionConfig.java \
        src/test/java/com/example/videoagent/config/PromptVersionConfigTest.java
git commit -m "feat: add PromptVersionConfig with TDD"
```

---

## Task 3: TDD - 实现 PromptTemplateService 服务类

**Files:**
- Create: `src/test/java/com/example/videoagent/service/PromptTemplateServiceTest.java`
- Create: `src/main/java/com/example/videoagent/service/PromptTemplateService.java`

**Step 1: Write the failing test - 基础加载测试**

```java
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
        // Given
        when(versionConfig.getDefaultVersion("chat")).thenReturn("v1");
        when(versionConfig.hasVersion("chat", "v2")).thenReturn(true);

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
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=PromptTemplateServiceTest -q`
Expected: FAIL with "PromptTemplateService not found"

**Step 3: Write minimal implementation**

```java
package com.example.videoagent.service;

import com.example.videoagent.config.PromptVersionConfig;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模板服务
 * 加载、缓存、渲染 .st 模板文件
 */
@Service
public class PromptTemplateService {

    private final PromptVersionConfig versionConfig;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateService(PromptVersionConfig versionConfig) {
        this.versionConfig = versionConfig;
    }

    /**
     * 渲染 Prompt 模板
     *
     * @param promptName Prompt 名称 (如 summarize, chat)
     * @param version    版本号，null 时使用默认版本
     * @param params     模板参数
     * @return 渲染后的 Prompt 字符串
     */
    public String render(String promptName, String version, Map<String, Object> params) {
        String actualVersion = version != null ? version : versionConfig.getDefaultVersion(promptName);
        String templateContent = loadTemplate(promptName, actualVersion);

        PromptTemplate promptTemplate = new PromptTemplate(templateContent);
        return promptTemplate.render(params);
    }

    /**
     * 使用默认版本渲染
     */
    public String render(String promptName, Map<String, Object> params) {
        return render(promptName, null, params);
    }

    /**
     * 加载模板内容（带缓存）
     */
    private String loadTemplate(String promptName, String version) {
        String cacheKey = promptName + "/" + version;

        return templateCache.computeIfAbsent(cacheKey, key -> {
            String path = "prompts/" + promptName + "/" + version + ".st";
            ClassPathResource resource = new ClassPathResource(path);

            if (!resource.exists()) {
                throw new IllegalArgumentException("Template not found: " + path);
            }

            try {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load template: " + path, e);
            }
        });
    }

    /**
     * 清除缓存（用于测试或热更新）
     */
    public void clearCache() {
        templateCache.clear();
    }
}
```

**Step 4: Run test to verify it fails (template not found)**

Run: `./mvnw test -Dtest=PromptTemplateServiceTest -q`
Expected: FAIL with "Template not found: prompts/summarize/v1.st"

**Step 5: 创建测试用 .st 模板文件**

```
src/main/resources/prompts/summarize/v1.st:
```

```st
基于以下字幕生成总结：
{subtitle}

请包含：1. 核心主题 2. 主要内容 3. 关键结论
```

```
src/main/resources/prompts/chat/v1.st:
```

```st
字幕内容：
{subtitle}

用户问题：{question}

请基于以上视频字幕回答问题。
```

```
src/main/resources/prompts/chat/v2.st:
```

```st
【优化版问答模板】

字幕内容：
{subtitle}

用户问题：{question}

请基于以上视频字幕回答问题，并引用时间戳。
```

**Step 6: Run test to verify it passes**

Run: `./mvnw test -Dtest=PromptTemplateServiceTest -q`
Expected: PASS (3 tests)

**Step 7: Commit**

```bash
git add src/main/java/com/example/videoagent/service/PromptTemplateService.java \
        src/test/java/com/example/videoagent/service/PromptTemplateServiceTest.java \
        src/main/resources/prompts/summarize/v1.st \
        src/main/resources/prompts/chat/v1.st \
        src/main/resources/prompts/chat/v2.st
git commit -m "feat: add PromptTemplateService with TDD"
```

---

## Task 4: 创建所有 .st 模板文件

**Files:**
- Create: `src/main/resources/prompts/summarize/v1.st`
- Create: `src/main/resources/prompts/chat/v1.st`
- Create: `src/main/resources/prompts/extract-concepts/v1.st`
- Create: `src/main/resources/prompts/intent-classification/v1.st`
- Create: `src/main/resources/prompts/extract-quotes/v1.st`
- Create: `src/main/resources/prompts/search-keyword/v1.st`
- Create: `src/main/resources/prompts/deep-qa/v1.st`

**Step 1: 创建 summarize/v1.st**

从 `PromptConstants.java` 的 `SUMMARIZE_PROMPT_TEMPLATE` 迁移，将 `%s` 替换为 `{subtitle}`

**Step 8: 验证所有模板文件创建**

Run: `ls -la src/main/resources/prompts/*/`
Expected: 所有目录下都有 v1.st 文件

**Step 9: Commit**

```bash
git add src/main/resources/prompts/
git commit -m "feat: add all .st template files"
```

---

## Task 5: 修改 VideoServiceImpl 使用 PromptTemplateService

**Files:**
- Modify: `src/main/java/com/example/videoagent/service/VideoServiceImpl.java`

**Step 1: 注入 PromptTemplateService**

在 VideoServiceImpl 中添加依赖注入：

```java
private final PromptTemplateService promptTemplateService;

// 构造函数中添加
public VideoServiceImpl(ChatModel chatModel, PromptTemplateService promptTemplateService) {
    this.chatModel = chatModel;
    this.promptTemplateService = promptTemplateService;
}
```

**Step 2: 修改 summarize 方法**

```java
public String summarize(String subtitle, String promptVersion) {
    String prompt = promptTemplateService.render("summarize", promptVersion,
        Map.of("subtitle", subtitle));
    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
}
```

**Step 3: 修改 chat 方法**

```java
public String chat(String subtitle, String question, String promptVersion) {
    String prompt = promptTemplateService.render("chat", promptVersion,
        Map.of("subtitle", subtitle, "question", question));
    return chatClient.prompt()
        .user(prompt)
        .call()
        .content();
}
```

**Step 4: 运行现有测试确保不破坏**

Run: `./mvnw test -Dtest=VideoServiceImplTest -q`
Expected: 可能需要更新测试以 mock PromptTemplateService

**Step 5: Commit**

```bash
git add src/main/java/com/example/videoagent/service/VideoServiceImpl.java
git commit -m "refactor: use PromptTemplateService in VideoServiceImpl"
```

---

## Task 6: 修改 Controller 支持 promptVersion 参数

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoController.java`

**Step 1: 添加 promptVersion 参数到相关方法**

```java
@PostMapping("/summarize")
public String summarize(
        @RequestParam String subtitleContent,
        @RequestParam(required = false) String promptVersion,
        Model model) {
    String summary = videoService.summarize(subtitleContent, promptVersion);
    model.addAttribute("summary", summary);
    model.addAttribute("subtitleContent", subtitleContent);
    model.addAttribute("subtitleLoaded", true);
    return "index";
}

@PostMapping("/ask")
public String ask(
        @RequestParam String subtitleContent,
        @RequestParam String question,
        @RequestParam(required = false) String promptVersion,
        @RequestParam(required = false, defaultValue = "false") boolean debug,
        Model model) {
    // ... 使用 promptVersion
}
```

**Step 2: 运行测试验证**

Run: `./mvnw test -q`
Expected: PASS

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/controller/VideoController.java
git commit -m "feat: add promptVersion parameter to controllers"
```

---

## Task 7: 运行所有测试验证

**Step 1: 运行全部测试**

Run: `./mvnw test -q`
Expected: All tests pass

**Step 2: 启动应用验证**

Run: `./mvnw spring-boot:run`
Then: `curl http://localhost:8080`
Expected: 应用正常启动，页面可访问

**Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete prompt version management system"
```

---

## Verification Checklist

- [ ] prompt-versions.yml 配置正确加载
- [ ] PromptVersionConfig 测试通过
- [ ] PromptTemplateService 测试通过
- [ ] 所有 .st 模板文件创建
- [ ] VideoServiceImpl 使用新服务
- [ ] Controller 支持 promptVersion 参数
- [ ] 所有测试通过
- [ ] 应用正常启动运行
