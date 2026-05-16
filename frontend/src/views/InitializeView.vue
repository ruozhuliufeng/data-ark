<template>
  <div class="init-page">
    <section class="init-hero">
      <div class="init-mark">
        <DataLine />
      </div>
      <h1>初始化 DataArk</h1>
      <p>首次启动需要指定一个 MySQL 数据库作为 DataArk 项目库，并创建第一个管理员成员。</p>
    </section>

    <section class="init-card">
      <div class="init-section-title">
        <h2>项目数据库</h2>
        <span>保存数据源、OSS 配置、任务、执行记录和项目成员</span>
      </div>

      <el-form label-width="116px">
        <div class="form-grid">
          <el-form-item label="MySQL 主机"><el-input v-model="form.host" placeholder="127.0.0.1" /></el-form-item>
          <el-form-item label="端口"><el-input-number v-model="form.port" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="数据库名"><el-input v-model="form.databaseName" placeholder="dataark" /></el-form-item>
          <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="数据库密码" class="full"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        </div>

        <div class="init-section-title member-title">
          <h2>管理员成员</h2>
          <span>初始化后写入项目库的第一个管理员</span>
        </div>

        <div class="form-grid">
          <el-form-item label="管理员账号"><el-input v-model="form.adminUsername" placeholder="admin" /></el-form-item>
          <el-form-item label="管理员密码"><el-input v-model="form.adminPassword" type="password" show-password /></el-form-item>
        </div>
      </el-form>

      <TestResultPanel :result="result" />

      <div class="init-actions">
        <el-button :icon="Refresh" @click="$emit('refresh')">重新检查状态</el-button>
        <el-button type="primary" :icon="CircleCheck" :loading="submitting" @click="submit">测试并保存初始化配置</el-button>
      </div>

      <el-alert
        class="init-alert"
        type="info"
        show-icon
        :closable="false"
        title="初始化保存后需要重启 DataArk。重启时应用会读取 config/dataark.properties，并切换到你配置的 MySQL 项目库。"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, DataLine, Refresh } from '@element-plus/icons-vue'
import { api, type InitializeRequest, type TestResult } from '../api/client'
import TestResultPanel from '../components/TestResultPanel.vue'

const emit = defineEmits<{
  refresh: []
}>()

const form = ref<InitializeRequest>({
  host: '127.0.0.1',
  port: 3306,
  databaseName: 'dataark',
  username: '',
  password: '',
  adminUsername: 'admin',
  adminPassword: ''
})
const submitting = ref(false)
const result = ref<TestResult | undefined>()

async function submit() {
  submitting.value = true
  try {
    result.value = await api.initialize(form.value)
    if (result.value.success) {
      ElMessage.success(result.value.message)
      emit('refresh')
    } else {
      ElMessage.error(result.value.message)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : String(error))
  } finally {
    submitting.value = false
  }
}
</script>
