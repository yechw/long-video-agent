# Meta-Prompting 功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 Prompt 优化器功能，让用户可以在聊天界面中优化自己的 Prompt，优化后立即使用。

**Architecture:** 后端新增 `/api/prompt/optimize` API 端点，使用 Meta-Prompting 模板调用 AI 进行优化；前端新增 `PromptOptimizer.vue` 对话框组件，集成到现有聊天界面。

**Tech Stack:** Spring Boot, Spring AI Alibaba, Vue 3, TypeScript

---

## Task 1: 创建 Meta-Prompting 模板文件

**Files:**
- Create: `src/main/resources/prompts/meta-optimize/v1.st`
- Modify: `src/main/resources/prompts/prompt-versions.yml`

**Step 1: 创建模板文件**

创建 `src/main/resources/prompts/meta-optimize/v1.st`:

```
你是一位专业的 Prompt Engineering 专家。

请根据以下要求优化用户提供的 Prompt：

【优化目标】
<goal>

【原始 Prompt】
<original_prompt>

【输出要求】
1. 输出优化后的完整 Prompt
2. 列出 2-5 条具体的改进说明
3. 使用以下 JSON 格式输出：

{
  "optimizedPrompt": "优化后的 Prompt 内容",
  "improvements": ["改进点1", "改进点2", ...]
}

注意：只输出 JSON，不要有任何其他内容。
```

**Step 2: 注册到版本配置**

修改 `src/main/resources/prompts/prompt-versions.yml`，添加：

```yaml
  meta-optimize:
    default: v1
    versions:
      v1:
        description: Meta-Prompting 优化模板
        created: 2026-02-28
```

**Step 3: Commit**

```bash
git add src/main/resources/prompts/meta-optimize/v1.st src/main/resources/prompts/prompt-versions.yml
git commit -m "feat: add meta-optimize prompt template"
```

---

## Task 2: 创建 DTO 类

**Files:**
- Create: `src/main/java/com/example/videoagent/dto/PromptOptimizeRequest.java`
- Create: `src/main/java/com/example/videoagent/dto/PromptOptimizeResponse.java`

**Step 1: 创建请求 DTO**

创建 `src/main/java/com/example/videoagent/dto/PromptOptimizeRequest.java`:

```java
package com.example.videoagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PromptOptimizeRequest {

    @NotBlank(message = "原始 Prompt 不能为空")
    @Size(max = 4000, message = "Prompt 长度不能超过 4000 字符")
    private String originalPrompt;

    @NotBlank(message = "优化目标不能为空")
    private String optimizationGoal; // CLEARER, CONCISE, STRICT, COMPLETE, CUSTOM

    @Size(max = 500, message = "自定义目标描述不能超过 500 字符")
    private String customGoal;

    // Getters and Setters
    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(String originalPrompt) {
        this.originalPrompt = originalPrompt;
    }

    public String getOptimizationGoal() {
        return optimizationGoal;
    }

    public void setOptimizationGoal(String optimizationGoal) {
        this.optimizationGoal = optimizationGoal;
    }

    public String getCustomGoal() {
        return customGoal;
    }

    public void setCustomGoal(String customGoal) {
        this.customGoal = customGoal;
    }
}
```

**Step 2: 创建响应 DTO**

创建 `src/main/java/com/example/videoagent/dto/PromptOptimizeResponse.java`:

```java
package com.example.videoagent.dto;

import java.util.List;

public class PromptOptimizeResponse {

    private String optimizedPrompt;
    private List<String> improvements;

    public PromptOptimizeResponse() {}

    public PromptOptimizeResponse(String optimizedPrompt, List<String> improvements) {
        this.optimizedPrompt = optimizedPrompt;
        this.improvements = improvements;
    }

    // Getters and Setters
    public String getOptimizedPrompt() {
        return optimizedPrompt;
    }

    public void setOptimizedPrompt(String optimizedPrompt) {
        this.optimizedPrompt = optimizedPrompt;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/example/videoagent/dto/PromptOptimizeRequest.java src/main/java/com/example/videoagent/dto/PromptOptimizeResponse.java
git commit -m "feat: add PromptOptimizeRequest and PromptOptimizeResponse DTOs"
```

---

## Task 3: 实现 Prompt 优化服务

**Files:**
- Create: `src/main/java/com/example/videoagent/service/PromptOptimizeService.java`
- Create: `src/test/java/com/example/videoagent/service/PromptOptimizeServiceTest.java`

**Step 1: 创建服务接口**

创建 `src/main/java/com/example/videoagent/service/PromptOptimizeService.java`:

```java
package com.example.videoagent.service;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.example.videoagent.dto.PromptOptimizeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptOptimizeService {

    private final ChatClient chatClient;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    public PromptOptimizeService(ChatClient.Builder chatClientBuilder,
                                  PromptTemplateService promptTemplateService,
                                  ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplateService = promptTemplateService;
        this.objectMapper = objectMapper;
    }

    public PromptOptimizeResponse optimize(PromptOptimizeRequest request) {
        String goalDescription = buildGoalDescription(
            request.getOptimizationGoal(),
            request.getCustomGoal()
        );

        String metaPrompt = promptTemplateService.render(
            "meta-optimize",
            null,
            Map.of(
                "original_prompt", request.getOriginalPrompt(),
                "goal", goalDescription
            )
        );

        String responseJson = chatClient.prompt()
            .user(metaPrompt)
            .call()
            .content();

        return parseResponse(responseJson);
    }

    private String buildGoalDescription(String goal, String customGoal) {
        return switch (goal.toUpperCase()) {
            case "CLEARER" -> "消除模糊表达，使用明确的动词和结构，添加具体的输入输出格式说明";
            case "CONCISE" -> "精简表达，去除重复和多余修饰词，合并冗余句子，减少 Token 使用量";
            case "STRICT" -> "添加强制性约束，明确要求基于上下文回答，禁止编造，增强防幻觉能力";
            case "COMPLETE" -> "补充 Few-Shot 示例，定义输出格式，添加边界情况处理说明";
            case "CUSTOM" -> customGoal != null ? customGoal : "根据用户需求进行优化";
            default -> "进行全面优化，提升 Prompt 质量和效果";
        };
    }

    private PromptOptimizeResponse parseResponse(String responseJson) {
        try {
            // 清理可能的 markdown 代码块标记
            String cleanJson = responseJson
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

            return objectMapper.readValue(cleanJson, PromptOptimizeResponse.class);
        } catch (Exception e) {
            // 如果解析失败，返回原始内容作为优化结果
            PromptOptimizeResponse fallback = new PromptOptimizeResponse();
            fallback.setOptimizedPrompt(responseJson);
            fallback.setImprovements(java.util.List.of("AI 返回格式异常，请手动检查结果"));
            return fallback;
        }
    }
}
```

**Step 2: 编写测试**

创建 `src/test/java/com/example/videoagent/service/PromptOptimizeServiceTest.java`:

```java
package com.example.videoagent.service;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.example.videoagent.dto.PromptOptimizeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PromptOptimizeServiceTest {

    @Autowired
    private PromptOptimizeService promptOptimizeService;

    @Test
    void testOptimizeWithClearerGoal() {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("请总结这个视频");
        request.setOptimizationGoal("CLEARER");

        PromptOptimizeResponse response = promptOptimizeService.optimize(request);

        assertNotNull(response);
        assertNotNull(response.getOptimizedPrompt());
        assertNotNull(response.getImprovements());
        assertFalse(response.getImprovements().isEmpty());
    }

    @Test
    void testBuildGoalDescription() {
        // This tests the private method indirectly through the service
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("test");
        request.setOptimizationGoal("CUSTOM");
        request.setCustomGoal("添加 JSON 格式");

        // Should not throw exception
        assertDoesNotThrow(() -> promptOptimizeService.optimize(request));
    }
}
```

**Step 3: 运行测试**

```bash
cd /Users/changweiye/workspace/long_video_agent_practice/LongVideoAgent
./mvnw test -Dtest=PromptOptimizeServiceTest -v
```

Expected: Tests may initially fail if AI service not available, but should run without compilation errors.

**Step 4: Commit**

```bash
git add src/main/java/com/example/videoagent/service/PromptOptimizeService.java src/test/java/com/example/videoagent/service/PromptOptimizeServiceTest.java
git commit -m "feat: implement PromptOptimizeService with meta-prompting"
```

---

## Task 4: 添加 API 端点

**Files:**
- Modify: `src/main/java/com/example/videoagent/controller/VideoApiController.java`

**Step 1: 注入服务并添加端点**

修改 `src/main/java/com/example/videoagent/controller/VideoApiController.java`:

```java
// Add to imports
import com.example.videoagent.dto.PromptOptimizeRequest;
import com.example.videoagent.dto.PromptOptimizeResponse;
import com.example.videoagent.service.PromptOptimizeService;
import jakarta.validation.Valid;

// Add to class fields
private final PromptOptimizeService promptOptimizeService;

// Update constructor
public VideoApiController(VideoService videoService,
                          IntentClassificationService intentClassificationService,
                          PromptOptimizeService promptOptimizeService) {
    this.videoService = videoService;
    this.intentClassificationService = intentClassificationService;
    this.promptOptimizeService = promptOptimizeService;
}

// Add new endpoint
@PostMapping("/prompt/optimize")
public VideoResponse optimizePrompt(@Valid @RequestBody PromptOptimizeRequest request) {
    try {
        PromptOptimizeResponse result = promptOptimizeService.optimize(request);
        return VideoResponse.success(result);
    } catch (Exception e) {
        log.error("Prompt 优化失败", e);
        return VideoResponse.error("优化失败: " + e.getMessage());
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/example/videoagent/controller/VideoApiController.java
git commit -m "feat: add /api/prompt/optimize endpoint"
```

---

## Task 5: 添加前端 API 封装

**Files:**
- Modify: `frontend/src/api/videoApi.ts`

**Step 1: 添加类型定义和 API 方法**

修改 `frontend/src/api/videoApi.ts`:

```typescript
// Add to types
export interface PromptOptimizeRequest {
  originalPrompt: string;
  optimizationGoal: 'CLEARER' | 'CONCISE' | 'STRICT' | 'COMPLETE' | 'CUSTOM';
  customGoal?: string;
}

export interface PromptOptimizeResponse {
  optimizedPrompt: string;
  improvements: string[];
}

// Add to videoApi object
optimizePrompt: async (request: PromptOptimizeRequest): Promise<ApiResponse<PromptOptimizeResponse>> => {
  const response = await fetch(`${API_BASE}/prompt/optimize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  return response.json();
},
```

**Step 2: Commit**

```bash
git add frontend/src/api/videoApi.ts
git commit -m "feat: add optimizePrompt API to videoApi"
```

---

## Task 6: 创建 PromptOptimizer 组件

**Files:**
- Create: `frontend/src/components/PromptOptimizer.vue`

**Step 1: 创建组件**

创建 `frontend/src/components/PromptOptimizer.vue`:

```vue
<template>
  <el-dialog
    v-model="visible"
    title="🔧 优化 Prompt"
    width="700px"
    :close-on-click-modal="false"
  >
    <div class="optimizer-content">
      <!-- 原始 Prompt -->
      <div class="section">
        <label>原始 Prompt:</label>
        <el-input
          v-model="originalPrompt"
          type="textarea"
          :rows="4"
          readonly
          class="readonly-input"
        />
      </div>

      <!-- 优化目标 -->
      <div class="section">
        <label>优化目标:</label>
        <el-radio-group v-model="selectedGoal">
          <el-radio label="CLEARER">更清晰</el-radio>
          <el-radio label="CONCISE">更简洁</el-radio>
          <el-radio label="STRICT">更严格</el-radio>
          <el-radio label="COMPLETE">更完整</el-radio>
          <el-radio label="CUSTOM">自定义</el-radio>
        </el-radio-group>
      </div>

      <!-- 自定义输入 -->
      <div v-if="selectedGoal === 'CUSTOM'" class="section">
        <label>自定义需求:</label>
        <el-input
          v-model="customGoal"
          type="textarea"
          :rows="2"
          placeholder="描述你的优化需求，例如：添加 JSON 格式要求，确保输出稳定"
        />
      </div>

      <!-- 优化按钮 -->
      <div class="section center">
        <el-button
          type="primary"
          :loading="optimizing"
          :disabled="!canOptimize"
          @click="handleOptimize"
        >
          {{ optimizing ? '优化中...' : '开始优化' }}
        </el-button>
      </div>

      <!-- 优化结果 -->
      <div v-if="result" class="section">
        <label>优化结果:</label>
        <el-input
          v-model="result.optimizedPrompt"
          type="textarea"
          :rows="6"
          class="result-input"
        />
      </div>

      <!-- 改进说明 -->
      <div v-if="result?.improvements?.length" class="section">
        <label>改进说明:</label>
        <ul class="improvements-list">
          <li v-for="(item, index) in result.improvements" :key="index">
            {{ item }}
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button
        type="primary"
        :disabled="!result"
        @click="handleUseOptimized"
      >
        使用优化版本
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { videoApi, type PromptOptimizeResponse } from '../api/videoApi';

const props = defineProps<{
  modelValue: boolean;
  originalPrompt: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'use', optimizedPrompt: string): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

const selectedGoal = ref('CLEARER');
const customGoal = ref('');
const optimizing = ref(false);
const result = ref<PromptOptimizeResponse | null>(null);

const canOptimize = computed(() => {
  if (selectedGoal.value === 'CUSTOM') {
    return customGoal.value.trim().length > 0;
  }
  return true;
});

const handleOptimize = async () => {
  optimizing.value = true;
  result.value = null;

  try {
    const response = await videoApi.optimizePrompt({
      originalPrompt: props.originalPrompt,
      optimizationGoal: selectedGoal.value as any,
      customGoal: customGoal.value || undefined,
    });

    if (response.success && response.data) {
      result.value = response.data;
    } else {
      ElMessage.error(response.message || '优化失败');
    }
  } catch (error) {
    ElMessage.error('请求失败，请稍后重试');
  } finally {
    optimizing.value = false;
  }
};

const handleUseOptimized = () => {
  if (result.value?.optimizedPrompt) {
    emit('use', result.value.optimizedPrompt);
    visible.value = false;
    resetState();
  }
};

const handleCancel = () => {
  visible.value = false;
  resetState();
};

const resetState = () => {
  selectedGoal.value = 'CLEARER';
  customGoal.value = '';
  result.value = null;
};
</script>

<style scoped>
.optimizer-content {
  padding: 0 10px;
}

.section {
  margin-bottom: 20px;
}

.section label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #606266;
}

.section.center {
  text-align: center;
}

.readonly-input :deep(.el-textarea__inner) {
  background-color: #f5f7fa;
}

.result-input :deep(.el-textarea__inner) {
  font-family: monospace;
}

.improvements-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
}

.improvements-list li {
  margin-bottom: 6px;
  line-height: 1.5;
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/components/PromptOptimizer.vue
git commit -m "feat: add PromptOptimizer Vue component"
```

---

## Task 7: 集成到 ChatPanel

**Files:**
- Modify: `frontend/src/components/ChatPanel.vue`

**Step 1: 导入组件并添加状态**

修改 `frontend/src/components/ChatPanel.vue`:

```vue
<script setup lang="ts">
// Add import
import PromptOptimizer from './PromptOptimizer.vue';

// Add to state
const optimizerVisible = ref(false);

// Add method
const openOptimizer = () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请先输入 Prompt');
    return;
  }
  optimizerVisible.value = true;
};

const handleUseOptimizedPrompt = (optimizedPrompt: string) => {
  inputMessage.value = optimizedPrompt;
  ElMessage.success('已使用优化后的 Prompt');
};
</script>
```

**Step 2: 在模板中添加优化按钮和组件**

```vue
<template>
  <!-- ... existing template ... -->

  <!-- 在输入框旁边添加优化按钮 -->
  <el-button
    v-if="inputMessage.trim()"
    @click="openOptimizer"
    :icon="Tools"
    circle
    title="优化 Prompt"
  />

  <!-- 添加优化器组件 -->
  <PromptOptimizer
    v-model="optimizerVisible"
    :original-prompt="inputMessage"
    @use="handleUseOptimizedPrompt"
  />
</template>
```

**Step 3: 导入图标**

```vue
<script setup lang="ts">
import { Tools } from '@element-plus/icons-vue';
</script>
```

**Step 4: Commit**

```bash
git add frontend/src/components/ChatPanel.vue
git commit -m "feat: integrate PromptOptimizer into ChatPanel"
```

---

## Task 8: 端到端测试

**Files:**
- Create: `src/test/java/com/example/videoagent/e2e/PromptOptimizeE2ETest.java`

**Step 1: 创建 E2E 测试**

创建 `src/test/java/com/example/videoagent/e2e/PromptOptimizeE2ETest.java`:

```java
package com.example.videoagent.e2e;

import com.example.videoagent.dto.PromptOptimizeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PromptOptimizeE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testOptimizePromptEndpoint() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("请总结这个视频的内容");
        request.setOptimizationGoal("CLEARER");

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.optimizedPrompt").exists())
            .andExpect(jsonPath("$.data.improvements").isArray());
    }

    @Test
    void testOptimizePromptWithCustomGoal() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        request.setOriginalPrompt("提取视频中的知识点");
        request.setOptimizationGoal("CUSTOM");
        request.setCustomGoal("添加 JSON 格式要求");

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testOptimizePromptValidation() throws Exception {
        PromptOptimizeRequest request = new PromptOptimizeRequest();
        // Missing required fields

        mockMvc.perform(post("/api/prompt/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

**Step 2: 运行测试**

```bash
./mvnw test -Dtest=PromptOptimizeE2ETest -v
```

**Step 3: Commit**

```bash
git add src/test/java/com/example/videoagent/e2e/PromptOptimizeE2ETest.java
git commit -m "test: add E2E tests for Prompt Optimize endpoint"
```

---

## Task 9: 验证和最终提交

**Step 1: 启动应用并测试**

```bash
# Terminal 1: Start backend
./mvnw spring-boot:run

# Terminal 2: Start frontend
cd frontend
npm run dev
```

**Step 2: 手动验证步骤**

1. 打开浏览器访问 `http://localhost:5173`
2. 上传示例字幕或输入字幕内容
3. 在聊天输入框输入简单 Prompt，如 "请总结视频"
4. 点击"🔧 优化"按钮
5. 选择不同优化目标，点击"开始优化"
6. 验证优化结果和改进说明显示正常
7. 点击"使用优化版本"，确认输入框内容被替换

**Step 3: 最终 Commit**

```bash
git add .
git commit -m "feat: complete meta-prompting feature implementation

- Add meta-optimize prompt template
- Add PromptOptimizeService with goal-based optimization
- Add /api/prompt/optimize endpoint
- Add PromptOptimizer Vue component
- Integrate into ChatPanel with optimize button
- Add comprehensive E2E tests"
```

---

## 完成总结

实现完成后，用户将能够：
1. 在聊天输入框输入 Prompt
2. 点击"🔧 优化"按钮打开优化对话框
3. 选择预设优化目标或输入自定义需求
4. 查看 AI 优化后的 Prompt 和改进说明
5. 一键使用优化后的版本继续对话
