package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.model.BackupJob;
import com.dataark.model.DataSourceConfig;
import com.dataark.model.DatabaseType;
import com.dataark.util.Strings2;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BackupCommandFactory {
    private final DataArkProperties properties;

    public BackupCommandFactory(DataArkProperties properties) {
        this.properties = properties;
    }

    public PreparedCommand buildDump(DataSourceConfig source, BackupJob job, File outputFile) {
        if (source.getType() == DatabaseType.MYSQL) {
            return buildMysqlDump(source, job, outputFile);
        }
        if (source.getType() == DatabaseType.POSTGRESQL) {
            return buildPostgresDump(source, job, outputFile);
        }
        throw new IllegalArgumentException("Oracle backup is reserved for a later version because expdp requires Oracle client setup.");
    }

    private PreparedCommand buildMysqlDump(DataSourceConfig source, BackupJob job, File outputFile) {
        List<String> command = new ArrayList<String>();
        command.add(properties.getCommand().getMysqldump());
        command.add("-h" + source.getHost());
        command.add("-P" + source.getPort());
        command.add("-u" + source.getUsername());
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add("--result-file=" + outputFile.getAbsolutePath());
        command.add(source.getDatabaseName());
        command.addAll(Strings2.commaList(job.getIncludeTables()));

        Map<String, String> env = new HashMap<String, String>();
        env.put("MYSQL_PWD", source.getPassword() == null ? "" : source.getPassword());
        return new PreparedCommand(command, env);
    }

    private PreparedCommand buildPostgresDump(DataSourceConfig source, BackupJob job, File outputFile) {
        List<String> command = new ArrayList<String>();
        command.add(properties.getCommand().getPgDump());
        command.add("-h");
        command.add(source.getHost());
        command.add("-p");
        command.add(String.valueOf(source.getPort()));
        command.add("-U");
        command.add(source.getUsername());
        command.add("-d");
        command.add(source.getDatabaseName());
        command.add("-F");
        command.add("p");
        command.add("-f");
        command.add(outputFile.getAbsolutePath());
        for (String table : Strings2.commaList(job.getIncludeTables())) {
            command.add("-t");
            command.add(table);
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("PGPASSWORD", source.getPassword() == null ? "" : source.getPassword());
        return new PreparedCommand(command, env);
    }

    public static class PreparedCommand {
        private final List<String> command;
        private final Map<String, String> environment;

        public PreparedCommand(List<String> command, Map<String, String> environment) {
            this.command = command;
            this.environment = environment;
        }

        public List<String> getCommand() {
            return command;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }
    }
}
