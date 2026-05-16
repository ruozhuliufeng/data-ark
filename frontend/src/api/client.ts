import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

export type DatabaseType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE'
export type StorageType = 'S3' | 'MINIO' | 'ALIYUN_OSS' | 'TENCENT_COS' | 'HUAWEI_OBS' | 'QINIU' | 'WEBDAV'
export type JobStatus = 'ENABLED' | 'DISABLED'

export interface Meta {
  databaseTypes: DatabaseType[]
  storageTypes: StorageType[]
  jobStatuses: JobStatus[]
}

export interface DataSourceConfig {
  id?: number
  name: string
  type: DatabaseType
  host: string
  port: number
  username: string
  password: string
  databaseName: string
  optionsJson?: string
}

export interface StorageConfig {
  id?: number
  name: string
  platform?: string
  enabled?: boolean
  type: StorageType
  bucket: string
  accessKey?: string
  secretKey?: string
  region?: string
  endpoint?: string
  domain?: string
  acl?: string
  pathStyleAccess?: boolean
  vendor?: string
  webdavUrl?: string
  webdavUsername?: string
  webdavPassword?: string
  multipartThresholdMb?: number
  multipartChunkMb?: number
  uploadConcurrency?: number
  uploadRetries?: number
  extraArgs?: string
  rcloneRemote?: string
  basePath: string
  configJson?: string
}

export interface BackupJob {
  id?: number
  name: string
  dataSourceId?: number
  storageId?: number
  cronExpression: string
  includeTables?: string
  excludeTables?: string
  gzipEnabled: boolean
  uploadEnabled: boolean
  retentionDays: number
  status: JobStatus
  lastRunAt?: string
  nextRunAt?: string
}

export interface ExecutionRecord {
  id: number
  jobId: number
  jobName: string
  status: 'RUNNING' | 'SUCCESS' | 'FAILED'
  startedAt?: string
  finishedAt?: string
  durationMillis?: number
  fileSize?: number
  localFile?: string
  remotePath?: string
  storageId?: number
  multipartUpload?: boolean
  totalParts?: number
  uploadedParts?: number
  manifestFile?: string
  message?: string
  commandLog?: string
}

export interface TestResult {
  success: boolean
  message: string
  detail?: string
  testedAt: string
}

export interface InitStatus {
  initialized: boolean
}

export interface InitializeRequest {
  host: string
  port: number
  databaseName: string
  username: string
  password: string
  adminUsername: string
  adminPassword: string
}

export const api = {
  initStatus: () => http.get<InitStatus, InitStatus>('/init/status'),
  initialize: (payload: InitializeRequest) => http.post<TestResult, TestResult>('/init', payload),
  meta: () => http.get<Meta, Meta>('/meta'),
  datasources: () => http.get<DataSourceConfig[], DataSourceConfig[]>('/datasources'),
  saveDatasource: (payload: DataSourceConfig) =>
    payload.id
      ? http.put<DataSourceConfig, DataSourceConfig>(`/datasources/${payload.id}`, payload)
      : http.post<DataSourceConfig, DataSourceConfig>('/datasources', payload),
  deleteDatasource: (id: number) => http.delete(`/datasources/${id}`),
  testDatasource: (payload: DataSourceConfig) => http.post<TestResult, TestResult>('/datasources/test', payload),
  testSavedDatasource: (id: number) => http.post<TestResult, TestResult>(`/datasources/${id}/test`),

  storages: () => http.get<StorageConfig[], StorageConfig[]>('/storages'),
  saveStorage: (payload: StorageConfig) =>
    payload.id
      ? http.put<StorageConfig, StorageConfig>(`/storages/${payload.id}`, payload)
      : http.post<StorageConfig, StorageConfig>('/storages', payload),
  deleteStorage: (id: number) => http.delete(`/storages/${id}`),
  testStorage: (payload: StorageConfig) => http.post<TestResult, TestResult>('/storages/test', payload),
  testSavedStorage: (id: number) => http.post<TestResult, TestResult>(`/storages/${id}/test`),
  uploadTestStorage: (payload: StorageConfig) => http.post<TestResult, TestResult>('/storages/upload-test', payload),
  uploadTestSavedStorage: (id: number) => http.post<TestResult, TestResult>(`/storages/${id}/upload-test`),

  jobs: () => http.get<BackupJob[], BackupJob[]>('/jobs'),
  saveJob: (payload: BackupJob) =>
    payload.id
      ? http.put<BackupJob, BackupJob>(`/jobs/${payload.id}`, payload)
      : http.post<BackupJob, BackupJob>('/jobs', payload),
  deleteJob: (id: number) => http.delete(`/jobs/${id}`),
  runJob: (id: number) => http.post(`/jobs/${id}/run`),

  executions: () => http.get<ExecutionRecord[], ExecutionRecord[]>('/executions'),
  resumeUpload: (id: number) => http.post<TestResult, TestResult>(`/executions/${id}/resume-upload`)
}
