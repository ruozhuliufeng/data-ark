<template>
  <div v-if="result" class="test-result" :class="{ success: result.success, failed: !result.success }">
    <div class="result-head">
      <el-tag :type="result.success ? 'success' : 'danger'">{{ result.success ? 'SUCCESS' : 'FAILED' }}</el-tag>
      <strong>{{ result.message }}</strong>
      <span>{{ result.testedAt }}</span>
    </div>
    <pre v-if="result.detail">{{ result.detail }}</pre>
  </div>
</template>

<script setup lang="ts">
import type { TestResult } from '../api/client'

defineProps<{
  result?: TestResult
}>()
</script>
