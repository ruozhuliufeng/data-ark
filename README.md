# DataArk

DataArk 是一个一体化数据库备份与对象存储同步平台。第一版目标是通过页面配置数据源、OSS/对象存储、备份任务和 Cron 定时计划，然后由后端执行 dump、压缩和 SDK 上传。

## 第一版范围

- 首次启动初始化：配置 DataArk 项目 MySQL 库，并创建管理员成员。
- 数据源配置：MySQL、PostgreSQL，Oracle 类型先预留。
- 存储目标配置：S3、MinIO、阿里云 OSS、腾讯云 COS、华为云 OBS、七牛云、WebDAV。
- 备份任务：整库或指定表，支持 gzip 压缩、手动执行、Cron 定时执行。
- 执行记录：状态、耗时、本地文件、远端路径、命令日志。
- 一体部署：Spring Boot 后端直接托管管理页面。

## 技术栈

- Java 8
- Spring Boot 2.7
- Spring Data JPA
- SQLite
- Vue 3 + TypeScript + Vite + Element Plus
- 对象存储 SDK：AWS S3、MinIO、阿里云 OSS、腾讯云 COS、华为云 OBS、七牛 Kodo、WebDAV
- mysqldump / pg_dump

当前仓库为了适配本机 JDK 1.8，使用 Spring Boot 2.7。后续如果部署环境切到 Java 21，可以升级到 Spring Boot 3。

## 本地启动

先构建前端：

```bash
cd frontend
npm install
npm run build
cd ..
```

再启动后端：

```bash
mvn spring-boot:run
```

访问：

```text
http://localhost:7001
```

默认数据目录：

```text
./config/dataark.properties
./data/dataark.db
./backup
./work
./logs
```

## Docker 一体部署

```bash
docker compose up -d --build
```

访问：

```text
http://服务器IP:7001
```

挂载目录：

```text
./config            初始化配置
./data              SQLite 数据库
./backup            本地备份文件
./work              任务工作目录
./logs              日志目录
./config/rclone     历史兼容目录，当前 SDK 上传不依赖 rclone.conf
```

## 首次初始化

第一次访问页面时会进入初始化向导，需要填写：

- DataArk 项目库 MySQL 主机、端口、数据库名、用户名、密码
- 管理员账号和密码

提交后系统会：

1. 使用 JDBC 测试 MySQL 连接。
2. 写入 `config/dataark.properties`。
3. 保存管理员账号的密码哈希。
4. 提示重启 DataArk。

重启后 DataArk 会使用该 MySQL 作为项目数据库，并自动初始化第一个管理员成员。

如果需要重新初始化，先备份数据，再删除：

```text
config/dataark.properties
```

## 存储配置

存储配置参考 `dromara/x-file-storage` 的平台化思路，但不依赖 `x-file-storage` 包本身。DataArk 内置各平台 SDK 适配器，页面只需要填写平台标识、类型、AK/SK、Bucket、地点/Region、Endpoint、基础路径等公共信息。

更多配置和验证说明见：

```text
docs/storage-testing.md
```

页面支持：

- 测试数据库连接：MySQL/PostgreSQL 使用 JDBC 执行 `select 1`
- 测试存储连通：通过对应平台 SDK 验证 Bucket 和基础路径
- 测试上传：生成临时探针文件并上传到 `{basePath}/_probe/`

## 备份命令依赖

DataArk 第一版调用数据库官方客户端：

```text
MySQL      -> mysqldump
PostgreSQL -> pg_dump
上传       -> 云厂商/协议 SDK
压缩       -> gzip
```

本机运行时需要提前安装数据库备份命令和 `gzip`。Docker 镜像会内置 `default-mysql-client`、`postgresql-client` 和 `gzip`；对象存储上传由 Java SDK 完成。

## Cron 说明

Spring Cron 使用 6 位格式：

```text
秒 分 时 日 月 周
```

示例：

```text
0 0 2 * * *     每天 02:00
0 */30 * * * *  每 30 分钟
0 0 3 * * MON   每周一 03:00
```

## 已知限制

- Oracle 仅预留类型，未默认集成 `expdp`。
- 排除表字段已预留，第一版 dump 命令暂未实现。
- 数据源和 OSS 密钥第一版明文保存在项目数据库，生产环境应增加加密存储。
- 前端构建产物由 Maven 打进 Spring Boot 静态资源，开发时也可单独运行 `npm run dev`。
