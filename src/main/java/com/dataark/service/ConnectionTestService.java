package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.dto.TestResult;
import com.dataark.model.DataSourceConfig;
import com.dataark.model.DatabaseType;
import com.dataark.model.StorageConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class ConnectionTestService {
    private final DataArkProperties properties;
    private final CommandRunner commandRunner;
    private final RcloneConfigService rcloneConfigService;

    public ConnectionTestService(DataArkProperties properties,
                                 CommandRunner commandRunner,
                                 RcloneConfigService rcloneConfigService) {
        this.properties = properties;
        this.commandRunner = commandRunner;
        this.rcloneConfigService = rcloneConfigService;
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
        RcloneConfigService.PreparedRclone prepared = null;
        try {
            prepared = rcloneConfigService.prepare(storage);
            List<String> command = new ArrayList<String>();
            command.add(properties.getCommand().getRclone());
            command.add("lsd");
            command.add(rcloneConfigService.remoteRoot(prepared.getRemoteName(), storage));
            command.add("--max-depth");
            command.add("1");
            return runRcloneCheck(command, prepared, "存储目标连接成功", "存储目标连接失败");
        } catch (Exception e) {
            return TestResult.fail("存储目标连接失败", e.getMessage());
        } finally {
            if (prepared != null) {
                prepared.cleanup();
            }
        }
    }

    public TestResult testStorageUpload(StorageConfig storage) {
        File probe = null;
        RcloneConfigService.PreparedRclone prepared = null;
        try {
            prepared = rcloneConfigService.prepare(storage);
            File workDir = new File(properties.getWorkDir());
            workDir.mkdirs();
            probe = File.createTempFile("dataark-oss-probe-", ".txt", workDir);
            FileWriter writer = new FileWriter(probe);
            writer.write("DataArk OSS upload probe\n");
            writer.write("storage=" + storage.getName() + "\n");
            writer.close();

            List<String> command = new ArrayList<String>();
            command.add(properties.getCommand().getRclone());
            command.add("copyto");
            command.add(probe.getAbsolutePath());
            command.add(rcloneConfigService.probeFile(prepared.getRemoteName(), storage, probe.getName()));
            command.add("--checksum");
            return runRcloneCheck(command, prepared, "测试文件上传成功", "测试文件上传失败");
        } catch (Exception e) {
            return TestResult.fail("测试文件上传失败", e.getMessage());
        } finally {
            if (prepared != null) {
                prepared.cleanup();
            }
            if (probe != null && probe.exists()) {
                probe.delete();
            }
        }
    }

    private TestResult runRcloneCheck(List<String> command,
                                      RcloneConfigService.PreparedRclone prepared,
                                      String ok,
                                      String fail) {
        CommandResult result = commandRunner.run(command, prepared.getEnvironment(), new File(properties.getWorkDir()));
        String detail = "$ " + StringUtils.join(command, " ") + System.lineSeparator() + result.getOutput();
        if (result.getExitCode() == 0) {
            return TestResult.ok(ok, detail);
        }
        if (result.getExitCode() == 127 && StringUtils.contains(result.getOutput(), "No such file")) {
            return TestResult.fail("未找到 rclone 命令，请先在 DataArk 运行环境安装 rclone", detail);
        }
        return TestResult.fail(fail + ", exitCode=" + result.getExitCode(), detail);
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
