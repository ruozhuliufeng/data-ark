<template>
  <InitializeView v-if="initialized === false" @refresh="loadInitStatus" />
  <div v-else-if="initialized === undefined" class="boot-screen">
    <div class="boot-box">
      <div class="brand-mark">
        <DataLine />
      </div>
      <strong>DataArk</strong>
      <span>正在检查初始化状态...</span>
    </div>
  </div>
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <DataLine />
        </div>
        <div>
          <div class="brand-name">DataArk</div>
          <div class="brand-sub">Backup Control</div>
        </div>
      </div>

      <button
        v-for="item in nav"
        :key="item.key"
        class="nav-item"
        :class="{ active: tab === item.key }"
        @click="tab = item.key"
      >
        <component :is="item.icon" />
        <span>{{ item.label }}</span>
      </button>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <div class="eyebrow">DataArk All-in-One</div>
          <h1>{{ activeTitle }}</h1>
        </div>
        <div class="top-actions">
          <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
          <el-button type="primary" :icon="VideoPlay" @click="quickRunFirstJob" :disabled="!jobs.length">运行首个任务</el-button>
        </div>
      </header>

      <section v-if="tab === 'dashboard'" class="dashboard">
        <div class="metrics">
          <div class="metric">
            <div class="metric-icon source"><Connection /></div>
            <div>
              <div class="metric-label">数据源</div>
              <div class="metric-value">{{ datasources.length }}</div>
            </div>
          </div>
          <div class="metric">
            <div class="metric-icon storage"><Cloudy /></div>
            <div>
              <div class="metric-label">存储目标</div>
              <div class="metric-value">{{ storages.length }}</div>
            </div>
          </div>
          <div class="metric">
            <div class="metric-icon job"><Timer /></div>
            <div>
              <div class="metric-label">定时任务</div>
              <div class="metric-value">{{ jobs.length }}</div>
            </div>
          </div>
          <div class="metric">
            <div class="metric-icon ok"><CircleCheck /></div>
            <div>
              <div class="metric-label">成功执行</div>
              <div class="metric-value">{{ successCount }}</div>
            </div>
          </div>
        </div>

        <div class="content-grid">
          <section class="panel">
            <div class="panel-title">
              <div>
                <h2>最近执行</h2>
                <p>备份、压缩、上传链路的最新状态</p>
              </div>
              <el-tag effect="plain">{{ executions.length }} 条</el-tag>
            </div>
            <el-table :data="executions.slice(0, 8)" border height="420" @row-click="showExecution">
              <el-table-column prop="jobName" label="任务" min-width="140" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="durationMillis" label="耗时(ms)" width="110" />
              <el-table-column prop="message" label="消息" min-width="160" />
            </el-table>
          </section>

          <section class="panel health-panel">
            <div class="panel-title">
              <div>
                <h2>配置闭环</h2>
                <p>保存前后都可以做真实连通验证</p>
              </div>
            </div>
            <div class="check-list">
              <div class="check-row">
                <Connection />
                <div>
                  <strong>数据库连接测试</strong>
                  <span>MySQL / PostgreSQL 使用 JDBC 执行 select 1</span>
                </div>
              </div>
              <div class="check-row">
                <Cloudy />
                <div>
                  <strong>多 OSS 连通测试</strong>
                  <span>填写 AK/SK、Bucket、地点后自动生成 rclone 配置验证</span>
                </div>
              </div>
              <div class="check-row">
                <Upload />
                <div>
                  <strong>上传测试文件</strong>
                  <span>生成临时探针文件并 copyto 到目标路径</span>
                </div>
              </div>
            </div>
          </section>
        </div>
      </section>

      <section v-if="tab === 'datasource'" class="panel">
        <div class="panel-title">
          <div>
            <h2>数据源配置</h2>
            <p>录入数据库连接信息，支持保存前测试连接</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="openDatasource()">新增数据源</el-button>
        </div>
        <el-table :data="datasources" border>
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column prop="type" label="类型" width="130" />
          <el-table-column prop="host" label="主机" min-width="150" />
          <el-table-column prop="port" label="端口" width="90" />
          <el-table-column prop="databaseName" label="库名" min-width="130" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Link" @click="testSavedDatasource(row)">测试</el-button>
              <el-button size="small" :icon="Edit" @click="openDatasource(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="removeDatasource(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="tab === 'storage'" class="panel">
        <div class="panel-title">
          <div>
            <h2>OSS / 对象存储</h2>
            <p>S3、MinIO、阿里云 OSS、腾讯云 COS、华为 OBS、七牛、WebDAV 统一通过 rclone 验证和上传</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="openStorage()">新增存储</el-button>
        </div>
        <el-table :data="storages" border>
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column prop="type" label="类型" width="140" />
          <el-table-column prop="bucket" label="Bucket" min-width="150" />
          <el-table-column prop="region" label="地点" min-width="130" />
          <el-table-column prop="endpoint" label="Endpoint" min-width="190" />
          <el-table-column prop="basePath" label="基础路径" min-width="150" />
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="Link" @click="testSavedStorage(row)">连通</el-button>
              <el-button size="small" :icon="Upload" @click="uploadTestSavedStorage(row)">上传</el-button>
              <el-button size="small" :icon="Edit" @click="openStorage(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="removeStorage(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="tab === 'job'" class="panel">
        <div class="panel-title">
          <div>
            <h2>备份任务</h2>
            <p>配置指定库、指定表、压缩、上传与 Cron 调度</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="openJob()">新增任务</el-button>
        </div>
        <el-table :data="jobs" border>
          <el-table-column prop="name" label="任务" min-width="140" />
          <el-table-column prop="cronExpression" label="Cron" min-width="130" />
          <el-table-column prop="includeTables" label="指定表" min-width="180" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastRunAt" label="上次执行" min-width="170" />
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" :icon="VideoPlay" @click="runJob(row)">执行</el-button>
              <el-button size="small" :icon="Edit" @click="openJob(row)">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="removeJob(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="tab === 'execution'" class="panel">
        <div class="panel-title">
          <div>
            <h2>执行记录</h2>
            <p>点击记录查看 dump、gzip、rclone 的完整命令日志</p>
          </div>
          <el-tag effect="plain">最近 100 条</el-tag>
        </div>
        <el-table :data="executions" border height="640" @row-click="showExecution">
          <el-table-column prop="jobName" label="任务" min-width="150" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="开始时间" min-width="170" />
          <el-table-column prop="finishedAt" label="完成时间" min-width="170" />
          <el-table-column prop="localFile" label="本地文件" min-width="240" />
          <el-table-column prop="remotePath" label="远端路径" min-width="260" />
          <el-table-column label="分片" width="120">
            <template #default="{ row }">
              <span v-if="row.multipartUpload">{{ row.uploadedParts || 0 }}/{{ row.totalParts || 0 }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="消息" min-width="180" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="canResumeUpload(row)"
                size="small"
                :icon="Upload"
                @click.stop="resumeUpload(row)"
              >
                继续上传
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>

    <el-dialog v-model="datasourceDialog" title="数据源配置" width="760px">
      <el-form label-width="108px">
        <div class="form-grid">
          <el-form-item label="名称"><el-input v-model="datasourceForm.name" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="datasourceForm.type" style="width: 100%" @change="applyDatabaseDefaultPort">
              <el-option v-for="item in meta.databaseTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="主机"><el-input v-model="datasourceForm.host" /></el-form-item>
          <el-form-item label="端口"><el-input-number v-model="datasourceForm.port" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="用户名"><el-input v-model="datasourceForm.username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="datasourceForm.password" type="password" show-password /></el-form-item>
          <el-form-item label="数据库" class="full"><el-input v-model="datasourceForm.databaseName" /></el-form-item>
          <el-form-item label="扩展配置" class="full">
            <el-input v-model="datasourceForm.optionsJson" type="textarea" :rows="3" placeholder='例如 {"ssl":false}' />
          </el-form-item>
        </div>
      </el-form>
      <TestResultPanel :result="lastTestResult" />
      <template #footer>
        <el-button @click="datasourceDialog = false">取消</el-button>
        <el-button :icon="Link" @click="testDatasourceDraft">测试连接</el-button>
        <el-button type="primary" @click="saveDatasource">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="storageDialog" title="存储目标配置" width="820px">
      <el-form label-width="126px">
        <div class="form-grid">
          <el-form-item label="名称"><el-input v-model="storageForm.name" /></el-form-item>
          <el-form-item label="类型">
            <el-select v-model="storageForm.type" style="width: 100%">
              <el-option v-for="item in meta.storageTypes" :key="item" :label="storageLabel(item)" :value="item" />
            </el-select>
          </el-form-item>
          <template v-if="storageForm.type !== 'WEBDAV'">
            <el-form-item label="AK"><el-input v-model="storageForm.accessKey" placeholder="Access Key / Secret ID" /></el-form-item>
            <el-form-item label="SK"><el-input v-model="storageForm.secretKey" type="password" show-password placeholder="Secret Key" /></el-form-item>
            <el-form-item label="Bucket"><el-input v-model="storageForm.bucket" /></el-form-item>
            <el-form-item label="地点">
              <el-input v-model="storageForm.region" :placeholder="regionPlaceholder" />
            </el-form-item>
            <el-form-item label="Endpoint" class="full">
              <el-input v-model="storageForm.endpoint" :placeholder="endpointPlaceholder" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="WebDAV 地址" class="full"><el-input v-model="storageForm.webdavUrl" placeholder="https://example.com/dav" /></el-form-item>
            <el-form-item label="账号"><el-input v-model="storageForm.webdavUsername" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="storageForm.webdavPassword" type="password" show-password /></el-form-item>
            <el-form-item label="根目录"><el-input v-model="storageForm.bucket" placeholder="留空表示 WebDAV 根目录" /></el-form-item>
            <el-form-item label="厂商"><el-input v-model="storageForm.vendor" placeholder="other / owncloud / nextcloud" /></el-form-item>
          </template>
          <el-form-item label="基础路径" class="full"><el-input v-model="storageForm.basePath" placeholder="/dataark" /></el-form-item>
          <el-form-item label="分片阈值(MB)"><el-input-number v-model="storageForm.multipartThresholdMb" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="分片大小(MB)"><el-input-number v-model="storageForm.multipartChunkMb" :min="5" style="width: 100%" /></el-form-item>
          <el-form-item label="失败重试次数"><el-input-number v-model="storageForm.uploadRetries" :min="1" :max="20" style="width: 100%" /></el-form-item>
          <el-form-item label="补充信息" class="full">
            <el-input v-model="storageForm.configJson" type="textarea" :rows="2" placeholder="可选：备注或后续扩展配置" />
          </el-form-item>
        </div>
      </el-form>
      <TestResultPanel :result="lastTestResult" />
      <template #footer>
        <el-button @click="storageDialog = false">取消</el-button>
        <el-button :icon="Link" @click="testStorageDraft">测试连通</el-button>
        <el-button :icon="Upload" @click="uploadTestStorageDraft">测试上传</el-button>
        <el-button type="primary" @click="saveStorage">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="jobDialog" title="备份任务配置" width="840px">
      <el-form label-width="110px">
        <div class="form-grid">
          <el-form-item label="任务名称"><el-input v-model="jobForm.name" /></el-form-item>
          <el-form-item label="Cron"><el-input v-model="jobForm.cronExpression" placeholder="0 0 2 * * *" /></el-form-item>
          <el-form-item label="数据源">
            <el-select v-model="jobForm.dataSourceId" style="width: 100%">
              <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="存储目标">
            <el-select v-model="jobForm.storageId" style="width: 100%">
              <el-option v-for="item in storages" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="指定表" class="full"><el-input v-model="jobForm.includeTables" placeholder="多个表用英文逗号分隔，留空表示整库" /></el-form-item>
          <el-form-item label="排除表" class="full"><el-input v-model="jobForm.excludeTables" placeholder="预留字段" /></el-form-item>
          <el-form-item label="压缩"><el-switch v-model="jobForm.gzipEnabled" /></el-form-item>
          <el-form-item label="上传"><el-switch v-model="jobForm.uploadEnabled" /></el-form-item>
          <el-form-item label="保留天数"><el-input-number v-model="jobForm.retentionDays" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="jobForm.status" style="width: 100%">
              <el-option v-for="item in meta.jobStatuses" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="jobDialog = false">取消</el-button>
        <el-button type="primary" @click="saveJob">保存并注册调度</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="executionDialog" title="执行日志" width="860px">
      <pre class="command-log">{{ executionDetail.commandLog || '暂无日志' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Cloudy,
  Connection,
  DataLine,
  Delete,
  Edit,
  Link,
  Plus,
  Refresh,
  Timer,
  Upload,
  VideoPlay
} from '@element-plus/icons-vue'
import { api, type BackupJob, type DataSourceConfig, type ExecutionRecord, type Meta, type StorageConfig, type StorageType, type TestResult } from './api/client'
import TestResultPanel from './components/TestResultPanel.vue'
import InitializeView from './views/InitializeView.vue'

type TabKey = 'dashboard' | 'datasource' | 'storage' | 'job' | 'execution'

const tab = ref<TabKey>('dashboard')
const initialized = ref<boolean | undefined>()
const meta = ref<Meta>({ databaseTypes: [], storageTypes: [], jobStatuses: [] })
const datasources = ref<DataSourceConfig[]>([])
const storages = ref<StorageConfig[]>([])
const jobs = ref<BackupJob[]>([])
const executions = ref<ExecutionRecord[]>([])
const datasourceDialog = ref(false)
const storageDialog = ref(false)
const jobDialog = ref(false)
const executionDialog = ref(false)
const datasourceForm = ref<DataSourceConfig>(emptyDatasource())
const storageForm = ref<StorageConfig>(emptyStorage())
const jobForm = ref<BackupJob>(emptyJob())
const executionDetail = ref<Partial<ExecutionRecord>>({})
const lastTestResult = ref<TestResult | undefined>()

const nav = [
  { key: 'dashboard', label: '运行概览', icon: DataLine },
  { key: 'datasource', label: '数据源', icon: Connection },
  { key: 'storage', label: '存储目标', icon: Cloudy },
  { key: 'job', label: '备份任务', icon: Timer },
  { key: 'execution', label: '执行记录', icon: CircleCheck }
] as const

const activeTitle = computed(() => nav.find(item => item.key === tab.value)?.label || 'DataArk')
const successCount = computed(() => executions.value.filter(item => item.status === 'SUCCESS').length)
const regionPlaceholder = computed(() => storageRegionPlaceholder(storageForm.value.type))
const endpointPlaceholder = computed(() => storageEndpointPlaceholder(storageForm.value.type))

onMounted(async () => {
  await loadInitStatus()
  if (initialized.value) {
    await loadAll()
  }
})

async function loadInitStatus() {
  try {
    const status = await api.initStatus()
    initialized.value = status.initialized
  } catch (error) {
    initialized.value = true
    notifyError(error)
  }
}

async function loadAll() {
  try {
    const [metaData, datasourceData, storageData, jobData, executionData] = await Promise.all([
      api.meta(),
      api.datasources(),
      api.storages(),
      api.jobs(),
      api.executions()
    ])
    meta.value = metaData
    datasources.value = datasourceData
    storages.value = storageData
    jobs.value = jobData
    executions.value = executionData
  } catch (error) {
    notifyError(error)
  }
}

function openDatasource(row?: DataSourceConfig) {
  datasourceForm.value = row ? { ...row } : emptyDatasource()
  lastTestResult.value = undefined
  datasourceDialog.value = true
}

async function saveDatasource() {
  try {
    await api.saveDatasource(datasourceForm.value)
    ElMessage.success('数据源已保存')
    datasourceDialog.value = false
    await loadAll()
  } catch (error) {
    notifyError(error)
  }
}

async function testDatasourceDraft() {
  try {
    lastTestResult.value = await api.testDatasource(datasourceForm.value)
    showTestMessage(lastTestResult.value)
  } catch (error) {
    notifyError(error)
  }
}

async function testSavedDatasource(row: DataSourceConfig) {
  if (!row.id) return
  try {
    const result = await api.testSavedDatasource(row.id)
    showTestMessage(result)
  } catch (error) {
    notifyError(error)
  }
}

async function removeDatasource(row: DataSourceConfig) {
  if (!row.id) return
  await confirmRemove()
  await api.deleteDatasource(row.id)
  ElMessage.success('数据源已删除')
  await loadAll()
}

function openStorage(row?: StorageConfig) {
  storageForm.value = normalizeStorage(row ? { ...row } : emptyStorage())
  lastTestResult.value = undefined
  storageDialog.value = true
}

async function saveStorage() {
  try {
    await api.saveStorage(storageForm.value)
    ElMessage.success('存储目标已保存')
    storageDialog.value = false
    await loadAll()
  } catch (error) {
    notifyError(error)
  }
}

async function testStorageDraft() {
  try {
    lastTestResult.value = await api.testStorage(storageForm.value)
    showTestMessage(lastTestResult.value)
  } catch (error) {
    notifyError(error)
  }
}

async function uploadTestStorageDraft() {
  try {
    lastTestResult.value = await api.uploadTestStorage(storageForm.value)
    showTestMessage(lastTestResult.value)
  } catch (error) {
    notifyError(error)
  }
}

async function testSavedStorage(row: StorageConfig) {
  if (!row.id) return
  try {
    const result = await api.testSavedStorage(row.id)
    showTestMessage(result)
  } catch (error) {
    notifyError(error)
  }
}

async function uploadTestSavedStorage(row: StorageConfig) {
  if (!row.id) return
  try {
    const result = await api.uploadTestSavedStorage(row.id)
    showTestMessage(result)
  } catch (error) {
    notifyError(error)
  }
}

function canResumeUpload(row: ExecutionRecord) {
  return row.status === 'FAILED' && Boolean(row.multipartUpload) && Boolean(row.localFile) && Boolean(row.remotePath)
}

async function resumeUpload(row: ExecutionRecord) {
  if (!row.id) return
  try {
    const result = await api.resumeUpload(row.id)
    showTestMessage(result)
    await loadAll()
  } catch (error) {
    notifyError(error)
  }
}

async function removeStorage(row: StorageConfig) {
  if (!row.id) return
  await confirmRemove()
  await api.deleteStorage(row.id)
  ElMessage.success('存储目标已删除')
  await loadAll()
}

function openJob(row?: BackupJob) {
  jobForm.value = row ? { ...row } : emptyJob()
  jobDialog.value = true
}

async function saveJob() {
  try {
    await api.saveJob(jobForm.value)
    ElMessage.success('任务已保存')
    jobDialog.value = false
    await loadAll()
  } catch (error) {
    notifyError(error)
  }
}

async function runJob(row: BackupJob) {
  if (!row.id) return
  try {
    await api.runJob(row.id)
    ElMessage.success('任务已提交')
    setTimeout(loadAll, 1200)
  } catch (error) {
    notifyError(error)
  }
}

async function quickRunFirstJob() {
  if (jobs.value[0]) {
    await runJob(jobs.value[0])
  }
}

async function removeJob(row: BackupJob) {
  if (!row.id) return
  await confirmRemove()
  await api.deleteJob(row.id)
  ElMessage.success('任务已删除')
  await loadAll()
}

function showExecution(row: ExecutionRecord) {
  executionDetail.value = row
  executionDialog.value = true
}

function statusType(status: ExecutionRecord['status']) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function applyDatabaseDefaultPort() {
  if (datasourceForm.value.type === 'MYSQL') datasourceForm.value.port = 3306
  if (datasourceForm.value.type === 'POSTGRESQL') datasourceForm.value.port = 5432
  if (datasourceForm.value.type === 'ORACLE') datasourceForm.value.port = 1521
}

function emptyDatasource(): DataSourceConfig {
  return { name: '', type: 'MYSQL', host: '', port: 3306, username: '', password: '', databaseName: '' }
}

function emptyStorage(): StorageConfig {
  return {
    name: '',
    type: 'MINIO',
    bucket: '',
    accessKey: '',
    secretKey: '',
    region: '',
    endpoint: '',
    vendor: '',
    webdavUrl: '',
    webdavUsername: '',
    webdavPassword: '',
    multipartThresholdMb: 100,
    multipartChunkMb: 64,
    uploadRetries: 3,
    basePath: '/dataark',
    configJson: ''
  }
}

function normalizeStorage(storage: StorageConfig): StorageConfig {
  return {
    ...emptyStorage(),
    ...storage,
    bucket: storage.bucket || '',
    basePath: storage.basePath || '/dataark',
    multipartThresholdMb: storage.multipartThresholdMb || 100,
    multipartChunkMb: storage.multipartChunkMb || 64,
    uploadRetries: storage.uploadRetries || 3
  }
}

function emptyJob(): BackupJob {
  return { name: '', cronExpression: '0 0 2 * * *', gzipEnabled: true, uploadEnabled: true, retentionDays: 30, status: 'ENABLED' }
}

function storageLabel(type: StorageType) {
  return {
    S3: 'Amazon S3',
    MINIO: 'MinIO',
    ALIYUN_OSS: '阿里云 OSS',
    TENCENT_COS: '腾讯云 COS',
    HUAWEI_OBS: '华为云 OBS',
    QINIU: '七牛云',
    WEBDAV: 'WebDAV'
  }[type]
}

function storageRegionPlaceholder(type: StorageType) {
  return {
    S3: '例如 ap-east-1',
    MINIO: '可选',
    ALIYUN_OSS: '例如 cn-hangzhou',
    TENCENT_COS: '例如 ap-guangzhou',
    HUAWEI_OBS: '例如 cn-east-3',
    QINIU: '例如 cn-east-1',
    WEBDAV: ''
  }[type]
}

function storageEndpointPlaceholder(type: StorageType) {
  return {
    S3: '可选，自定义 S3 兼容地址时填写',
    MINIO: '必填，例如 http://127.0.0.1:9000',
    ALIYUN_OSS: '可选，默认按地点生成 oss-{地点}.aliyuncs.com',
    TENCENT_COS: '可选，默认按地点生成 cos.{地点}.myqcloud.com',
    HUAWEI_OBS: '可选，默认按地点生成 obs.{地点}.myhuaweicloud.com',
    QINIU: '可选，默认按地点生成 s3-{地点}.qiniucs.com',
    WEBDAV: ''
  }[type]
}

function showTestMessage(result: TestResult) {
  if (result.success) {
    ElMessage.success(result.message)
  } else {
    ElMessage.error(result.message)
  }
}

async function confirmRemove() {
  await ElMessageBox.confirm('确认删除这条配置？', '删除确认', { type: 'warning' })
}

function notifyError(error: unknown) {
  ElMessage.error(error instanceof Error ? error.message : String(error))
}
</script>
