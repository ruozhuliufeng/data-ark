package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.dto.TestResult;
import com.dataark.model.DataSourceConfig;
import com.dataark.model.DatabaseType;
import com.dataark.model.StorageConfig;
import com.dataark.service.storage.ObjectStorageClient;
import com.dataark.service.storage.ObjectStorageClientFactory;
import com.dataark.service.storage.StorageObjectKeyService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

@Service
public class ConnectionTestService {
    private final DataArkProperties properties;
    private final ObjectStorageClientFactory clientFactory;
    private final StorageObjectKeyService storageObjectKeyService;

    public ConnectionTestService(DataArkProperties properties,
                                 ObjectStorageClientFactory clientFactory,
                                 StorageObjectKeyService storageObjectKeyService) {
        this.properties = properties;
        this.clientFactory = clientFactory;
        this.storageObjectKeyService = storageObjectKeyService;
    }

    public TestResult testDataSource(DataSourceConfig source) {
        if (source.getType() == DatabaseType.ORACLE) {
            return TestResult.fail("Oracle 连接测试暂未启用", "第一版未内置 Oracle JDBC 驱动，请后续配置 Oracle JDBC 和 expdp 环境。");
        }

        String url = jdbcUrl(source);
        Properties props = new Properties();
        props.setProperty("user", source.getUsername());
        props.setProperty("password", source.getPassword() == null ? "" : source.getPassword());
        props.setProperty("connectTimeout", "5000");
        props.setProperty("socketTimeout", "5000");

        try (Connection connection = DriverManager.getConnection(url, props);
             Statement statement = connection.createStatement()) {
            statement.execute("select 1");
            return TestResult.ok("数据库连接成功", url);
        } catch (Exception e) {
            return TestResult.fail("数据库连接失败", e.getMessage());
        }
    }

    public TestResult testStorageList(StorageConfig storage) {
        try {
            ObjectStorageClient client = clientFactory.create(storage);
            client.checkRoot(storageObjectKeyService.normalizePath(storage.getBasePath()));
            return TestResult.ok("存储目标连接成功", "已通过 " + storage.getType() + " SDK 验证 Bucket 和基础路径。");
        } catch (Exception e) {
            return TestResult.fail("存储目标连接失败", e.getMessage());
        }
    }

    public TestResult testStorageUpload(StorageConfig storage) {
        File probe = null;
        try {
            ObjectStorageClient client = clientFactory.create(storage);
            File workDir = new File(properties.getWorkDir());
            workDir.mkdirs();
            probe = File.createTempFile("dataark-oss-probe-", ".txt", workDir);
            FileWriter writer = new FileWriter(probe);
            writer.write("DataArk OSS upload probe\n");
            writer.write("storage=" + storage.getName() + "\n");
            writer.close();

            String objectKey = storageObjectKeyService.probeKey(storage, probe.getName());
            client.put(objectKey, probe);
            String displayPath = storageObjectKeyService.displayPath(storage, objectKey);
            return TestResult.ok("测试文件上传成功", "SDK upload " + probe.getAbsolutePath() + " -> " + displayPath);
        } catch (Exception e) {
            return TestResult.fail("测试文件上传失败", e.getMessage());
        } finally {
            if (probe != null && probe.exists()) {
                probe.delete();
            }
        }
    }

    private String jdbcUrl(DataSourceConfig source) {
        if (source.getType() == DatabaseType.MYSQL) {
            return "jdbc:mysql://" + source.getHost() + ":" + source.getPort() + "/" + source.getDatabaseName()
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (source.getType() == DatabaseType.POSTGRESQL) {
            return "jdbc:postgresql://" + source.getHost() + ":" + source.getPort() + "/" + source.getDatabaseName();
        }
        throw new IllegalArgumentException("Unsupported database type: " + source.getType());
    }

}
