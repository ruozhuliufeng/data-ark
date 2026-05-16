package com.dataark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@EnableScheduling
@SpringBootApplication
public class DataArkApplication {

    public static void main(String[] args) {
        mkdir(env("DATAARK_DATA_DIR", "./data"));
        mkdir(parent(env("DATAARK_CONFIG_FILE", "./config/dataark.properties")));
        mkdir(env("DATAARK_WORK_DIR", "./work"));
        mkdir(env("DATAARK_BACKUP_DIR", "./backup"));
        mkdir(env("DATAARK_LOG_DIR", "./logs"));
        SpringApplication.run(DataArkApplication.class, args);
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static void mkdir(String path) {
        new File(path).mkdirs();
    }

    private static String parent(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        return parent == null ? "." : parent.getPath();
    }
}
