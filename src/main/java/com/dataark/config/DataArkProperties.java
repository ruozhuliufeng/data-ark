package com.dataark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dataark")
public class DataArkProperties {
    private boolean initialized = false;
    private String configFile = "./config/dataark.properties";
    private String workDir = "./work";
    private String backupDir = "./backup";
    private String logDir = "./logs";
    private int backupConcurrency = 1;
    private int gzipLevel = 1;
    private Command command = new Command();

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public int getBackupConcurrency() {
        return backupConcurrency;
    }

    public void setBackupConcurrency(int backupConcurrency) {
        this.backupConcurrency = backupConcurrency;
    }

    public int getGzipLevel() {
        return gzipLevel;
    }

    public void setGzipLevel(int gzipLevel) {
        this.gzipLevel = gzipLevel;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public static class Command {
        private String mysqldump = "mysqldump";
        private String pgDump = "pg_dump";
        private String rclone = "rclone";

        public String getMysqldump() {
            return mysqldump;
        }

        public void setMysqldump(String mysqldump) {
            this.mysqldump = mysqldump;
        }

        public String getPgDump() {
            return pgDump;
        }

        public void setPgDump(String pgDump) {
            this.pgDump = pgDump;
        }

        public String getRclone() {
            return rclone;
        }

        public void setRclone(String rclone) {
            this.rclone = rclone;
        }
    }
}
