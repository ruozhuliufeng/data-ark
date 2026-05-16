package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.model.StorageConfig;
import com.dataark.model.StorageType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class RcloneConfigService {
    private final DataArkProperties properties;

    public RcloneConfigService(DataArkProperties properties) {
        this.properties = properties;
    }

    public PreparedRclone prepare(StorageConfig storage) throws IOException {
        File workDir = new File(properties.getWorkDir());
        workDir.mkdirs();

        String remoteName = remoteName(storage);
        File configFile = File.createTempFile("dataark-rclone-", ".conf", workDir);
        FileWriter writer = new FileWriter(configFile);
        try {
            writer.write(buildConfig(remoteName, storage));
        } finally {
            writer.close();
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("RCLONE_CONFIG", configFile.getAbsolutePath());
        return new PreparedRclone(remoteName, configFile, env);
    }

    public String remoteRoot(String remoteName, StorageConfig storage) {
        String bucket = StringUtils.defaultString(storage.getBucket()).trim();
        if (storage.getType() == StorageType.WEBDAV) {
            return remoteName + ":" + trimSlashes(bucket);
        }
        return remoteName + ":" + bucket;
    }

    public String remoteFile(String remoteName, StorageConfig storage, String fileName) {
        String basePath = normalizePath(storage.getBasePath());
        String root = remoteRoot(remoteName, storage);
        if (StringUtils.isBlank(basePath)) {
            return root + "/" + fileName;
        }
        return root + "/" + basePath + "/" + fileName;
    }

    public String probeFile(String remoteName, StorageConfig storage, String fileName) {
        String basePath = normalizePath(storage.getBasePath());
        String root = remoteRoot(remoteName, storage);
        if (StringUtils.isBlank(basePath)) {
            return root + "/_probe/" + fileName;
        }
        return root + "/" + basePath + "/_probe/" + fileName;
    }

    private String buildConfig(String remoteName, StorageConfig storage) {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(remoteName).append("]").append(System.lineSeparator());
        if (storage.getType() == StorageType.WEBDAV) {
            appendWebdav(builder, storage);
        } else {
            appendS3(builder, storage);
        }
        return builder.toString();
    }

    private void appendS3(StringBuilder builder, StorageConfig storage) {
        builder.append("type = s3").append(System.lineSeparator());
        builder.append("provider = ").append(provider(storage)).append(System.lineSeparator());
        appendIfPresent(builder, "access_key_id", storage.getAccessKey());
        appendIfPresent(builder, "secret_access_key", storage.getSecretKey());
        appendIfPresent(builder, "region", storage.getRegion());
        appendIfPresent(builder, "endpoint", endpoint(storage));
        builder.append("acl = private").append(System.lineSeparator());
    }

    private void appendWebdav(StringBuilder builder, StorageConfig storage) {
        builder.append("type = webdav").append(System.lineSeparator());
        appendIfPresent(builder, "url", storage.getWebdavUrl());
        appendIfPresent(builder, "vendor", StringUtils.defaultIfBlank(storage.getVendor(), "other"));
        appendIfPresent(builder, "user", storage.getWebdavUsername());
        appendIfPresent(builder, "pass", storage.getWebdavPassword());
    }

    private String provider(StorageConfig storage) {
        if (storage.getType() == StorageType.MINIO) {
            return "Minio";
        }
        if (storage.getType() == StorageType.ALIYUN_OSS) {
            return "Alibaba";
        }
        if (storage.getType() == StorageType.TENCENT_COS) {
            return "TencentCOS";
        }
        if (storage.getType() == StorageType.HUAWEI_OBS) {
            return "HuaweiOBS";
        }
        if (storage.getType() == StorageType.QINIU) {
            return "Qiniu";
        }
        return StringUtils.defaultIfBlank(storage.getVendor(), "AWS");
    }

    private String endpoint(StorageConfig storage) {
        if (StringUtils.isNotBlank(storage.getEndpoint())) {
            return storage.getEndpoint();
        }
        String region = StringUtils.trimToEmpty(storage.getRegion());
        if (StringUtils.isBlank(region)) {
            return "";
        }
        if (storage.getType() == StorageType.ALIYUN_OSS) {
            return "oss-" + region + ".aliyuncs.com";
        }
        if (storage.getType() == StorageType.TENCENT_COS) {
            return "cos." + region + ".myqcloud.com";
        }
        if (storage.getType() == StorageType.HUAWEI_OBS) {
            return "obs." + region + ".myhuaweicloud.com";
        }
        if (storage.getType() == StorageType.QINIU) {
            return "s3-" + region + ".qiniucs.com";
        }
        return "";
    }

    private String remoteName(StorageConfig storage) {
        if (StringUtils.isNotBlank(storage.getRcloneRemote())) {
            return storage.getRcloneRemote().trim();
        }
        String id = storage.getId() == null ? "draft" : String.valueOf(storage.getId());
        return "dataark-" + storage.getType().name().toLowerCase().replace('_', '-') + "-" + id;
    }

    private void appendIfPresent(StringBuilder builder, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            builder.append(key).append(" = ").append(value.trim()).append(System.lineSeparator());
        }
    }

    private String normalizePath(String value) {
        return trimSlashes(StringUtils.defaultIfBlank(value, "/"));
    }

    private String trimSlashes(String value) {
        String path = StringUtils.defaultString(value).trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static class PreparedRclone {
        private final String remoteName;
        private final File configFile;
        private final Map<String, String> environment;

        public PreparedRclone(String remoteName, File configFile, Map<String, String> environment) {
            this.remoteName = remoteName;
            this.configFile = configFile;
            this.environment = environment;
        }

        public String getRemoteName() {
            return remoteName;
        }

        public File getConfigFile() {
            return configFile;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public void cleanup() {
            if (configFile != null && configFile.exists()) {
                configFile.delete();
            }
        }
    }
}
