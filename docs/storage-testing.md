# 多 OSS 配置与验证

DataArk 统一通过 rclone 对接对象存储。配置方式参考 `x-file-storage` 的平台化思路：每个存储目标都有稳定的“平台标识”、启用状态、访问凭证、Bucket、Endpoint、基础路径和上传策略。后端会在测试和上传时自动生成临时 rclone 配置，不需要手工维护 remote。

## 页面字段

| 字段 | 说明 |
| --- | --- |
| 名称 | DataArk 内部展示名 |
| 平台标识 | 存储平台唯一标识，例如 `qiniu-prod`、`minio-local`，会作为内部 remote 名称 |
| 启用 | 关闭后不允许测试或上传 |
| 类型 | S3、MinIO、阿里云 OSS、腾讯云 COS、华为 OBS、七牛、WebDAV |
| AK | S3 类存储的 Access Key；腾讯云可填 SecretId |
| SK | S3 类存储的 Secret Key |
| Bucket / 根目录 | S3 类存储填写 bucket；WebDAV 可填写远端根目录 |
| 地点 | Region，例如七牛 `cn-east-1`、阿里云 `cn-hangzhou` |
| Endpoint | MinIO 必填；阿里云、腾讯云、华为云、七牛可按地点自动生成，也可手动覆盖 |
| 访问域名 | 可选，记录 CDN 或公开访问域名，便于后续展示/下载扩展 |
| ACL | 默认 `private`，可按存储平台填写 `public-read` 等 |
| 路径样式访问 | S3 兼容存储需要 path-style 时开启，例如部分 MinIO 部署 |
| 基础路径 | DataArk 上传备份文件的业务路径，例如 `/dataark/prod` |
| 分片阈值(MB) | 备份文件达到该大小后启用 DataArk 分片上传 |
| 分片大小(MB) | 每个 part 文件大小，默认 64MB，最小 5MB |
| 上传并发 | rclone S3 multipart 上传并发，默认 4 |
| 失败重试次数 | 每次 rclone 上传 part 的重试次数 |
| rclone 参数 | 可选高级参数，例如 `--transfers 4 --checkers 8` |

## 验证动作

页面提供两个验证动作：

- 测试连通：生成临时 rclone 配置后执行 `rclone lsd remote:bucket --max-depth 1`
- 测试上传：生成临时探针文件后执行 `rclone copyto local remote:bucket/basePath/_probe/file`

测试上传成功后，远端会出现：

```text
{basePath}/_probe/dataark-oss-probe-xxxx.txt
```

## 大文件分片与断点续传

当备份文件达到“分片阈值”后，DataArk 会先在 `work/multipart-parts/` 下切分 part 文件，再逐片上传到：

```text
{remotePath}.parts/{fileName}.part00000
```

上传过程会在 `work/multipart-manifests/` 生成 manifest，记录源文件、远端路径、分片大小和已上传 part。若任务中断或失败，只要本地备份文件和 manifest 仍存在，执行记录页会显示“继续上传”，点击后会跳过远端已经存在的 part，只补传剩余分片。

底层 rclone 命令还会附带：

```text
--s3-upload-cutoff
--s3-chunk-size
--s3-upload-concurrency
--s3-leave-parts-on-error
--retries
--low-level-retries
```

## 本机 rclone 依赖

DataArk 运行环境必须能直接执行 `rclone` 命令。macOS 可用：

```bash
brew install rclone
```

Docker 镜像会内置 rclone。手工部署时，也可以通过环境变量指定自定义路径：

```bash
export DATAARK_RCLONE=/usr/local/bin/rclone
```

当前页面不会再要求提前维护 `rclone.conf`。如需排查，也可参考示例文件：

```text
config/rclone/rclone.conf.example
```
