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
