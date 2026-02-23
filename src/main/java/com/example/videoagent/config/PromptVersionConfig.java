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
