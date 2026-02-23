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
