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
