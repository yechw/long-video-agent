<template>
  <div class="card">
    <div class="card-header">知识概念时间轴</div>

    <el-timeline>
      <el-timeline-item
        v-for="(concept, index) in concepts"
        :key="index"
        :timestamp="concept.timestampFrom"
        placement="top"
      >
        <el-card>
          <h4>{{ concept.concept }}</h4>
          <p>{{ concept.description }}</p>
          <div class="time-range">
            <el-tag size="small" type="info">
              {{ concept.timestampFrom }} - {{ concept.timestampTo }}
            </el-tag>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import type { Concept } from '../types'

defineProps<{
  concepts: Concept[]
}>()
</script>

<style scoped>
.el-card h4 {
  margin: 0 0 8px;
  color: #303133;
}

.el-card p {
  margin: 0 0 8px;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.time-range {
  margin-top: 8px;
}
</style>
