package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.dto.InitializeRequest;
import com.dataark.dto.TestResult;
import com.dataark.model.DataSourceConfig;
import com.dataark.model.DatabaseType;
import com.dataark.model.MemberRole;
import com.dataark.model.ProjectMember;
import com.dataark.repository.ProjectMemberRepository;
import com.dataark.util.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;

@Service
public class InitializationService implements ApplicationRunner {
    private final DataArkProperties properties;
    private final ConnectionTestService connectionTestService;
    private final ProjectMemberRepository memberRepository;
    private final String adminUsername;
    private final String adminPasswordHash;

    public InitializationService(DataArkProperties properties,
                                 ConnectionTestService connectionTestService,
                                 ProjectMemberRepository memberRepository,
                                 @Value("${dataark.admin.username:}") String adminUsername,
                                 @Value("${dataark.admin.password-hash:}") String adminPasswordHash) {
        this.properties = properties;
        this.connectionTestService = connectionTestService;
        this.memberRepository = memberRepository;
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPasswordHash;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isInitialized() && adminUsername != null && !adminUsername.trim().isEmpty()) {
            seedAdminMember();
        }
    }

    public boolean isInitialized() {
        return properties.isInitialized();
    }

    public TestResult initialize(InitializeRequest request) {
        DataSourceConfig source = new DataSourceConfig();
        source.setName("DataArk Project Database");
        source.setType(DatabaseType.MYSQL);
        source.setHost(request.getHost());
        source.setPort(request.getPort());
        source.setDatabaseName(request.getDatabaseName());
        source.setUsername(request.getUsername());
        source.setPassword(request.getPassword());

        TestResult connectionResult = connectionTestService.testDataSource(source);
        if (!connectionResult.isSuccess()) {
            return connectionResult;
        }

        try {
            writeConfig(request);
            return TestResult.ok("初始化配置已保存，请重启 DataArk 生效", properties.getConfigFile());
        } catch (Exception e) {
            return TestResult.fail("初始化配置保存失败", e.getMessage());
        }
    }

    private void seedAdminMember() {
        if (memberRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }
        ProjectMember member = new ProjectMember();
        member.setUsername(adminUsername);
        member.setPasswordHash(adminPasswordHash);
        member.setRole(MemberRole.ADMIN);
        member.setEnabled(true);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    private void writeConfig(InitializeRequest request) throws Exception {
        File file = new File(properties.getConfigFile());
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        Properties config = new Properties();
        config.setProperty("DATAARK_INITIALIZED", "true");
        config.setProperty("DATAARK_DB_DRIVER", "com.mysql.cj.jdbc.Driver");
        config.setProperty("DATAARK_DB_URL", jdbcUrl(request));
        config.setProperty("DATAARK_DB_USERNAME", request.getUsername());
        config.setProperty("DATAARK_DB_PASSWORD", request.getPassword());
        config.setProperty("DATAARK_DB_DIALECT", "org.hibernate.dialect.MySQL8Dialect");
        config.setProperty("dataark.admin.username", request.getAdminUsername());
        config.setProperty("dataark.admin.password-hash", PasswordHasher.hash(request.getAdminPassword()));

        FileOutputStream out = new FileOutputStream(file);
        try {
            config.store(out, "DataArk initialization config");
        } finally {
            out.close();
        }
    }

    private String jdbcUrl(InitializeRequest request) {
        return "jdbc:mysql://" + request.getHost() + ":" + request.getPort() + "/" + request.getDatabaseName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    }
}
