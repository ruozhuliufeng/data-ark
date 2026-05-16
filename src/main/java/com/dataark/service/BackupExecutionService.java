package com.dataark.service;

import com.dataark.config.DataArkProperties;
import com.dataark.model.BackupJob;
import com.dataark.model.DataSourceConfig;
import com.dataark.model.ExecutionRecord;
import com.dataark.model.ExecutionStatus;
import com.dataark.model.StorageConfig;
import com.dataark.repository.BackupJobRepository;
import com.dataark.repository.DataSourceConfigRepository;
import com.dataark.repository.ExecutionRecordRepository;
import com.dataark.repository.StorageConfigRepository;
import com.dataark.util.DateTimes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.List;

@Service
public class BackupExecutionService {
    private final DataArkProperties properties;
    private final BackupJobRepository backupJobRepository;
    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final StorageConfigRepository storageConfigRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final BackupCommandFactory commandFactory;
    private final CommandRunner commandRunner;
    private final RcloneConfigService rcloneConfigService;
    private final ResumableUploadService resumableUploadService;

    public BackupExecutionService(DataArkProperties properties,
                                  BackupJobRepository backupJobRepository,
                                  DataSourceConfigRepository dataSourceConfigRepository,
                                  StorageConfigRepository storageConfigRepository,
                                  ExecutionRecordRepository executionRecordRepository,
                                  BackupCommandFactory commandFactory,
                                  CommandRunner commandRunner,
                                  RcloneConfigService rcloneConfigService,
                                  ResumableUploadService resumableUploadService) {
        this.properties = properties;
        this.backupJobRepository = backupJobRepository;
        this.dataSourceConfigRepository = dataSourceConfigRepository;
        this.storageConfigRepository = storageConfigRepository;
        this.executionRecordRepository = executionRecordRepository;
        this.commandFactory = commandFactory;
        this.commandRunner = commandRunner;
        this.rcloneConfigService = rcloneConfigService;
        this.resumableUploadService = resumableUploadService;
    }

    @PostConstruct
    public void initDirs() {
        new File(properties.getBackupDir()).mkdirs();
        new File(properties.getWorkDir()).mkdirs();
        new File(properties.getLogDir()).mkdirs();
    }

    @Async
    public void runAsync(Long jobId) {
        runNow(jobId);
    }

    @Transactional
    public ExecutionRecord runNow(Long jobId) {
        BackupJob job = backupJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Backup job not found: " + jobId));
        ExecutionRecord record = new ExecutionRecord();
        record.setJobId(job.getId());
        record.setJobName(job.getName());
        record.setStatus(ExecutionStatus.RUNNING);
        record.setStartedAt(LocalDateTime.now());
        executionRecordRepository.save(record);

        StringBuilder commandLog = new StringBuilder();
        try {
            DataSourceConfig source = dataSourceConfigRepository.findById(job.getDataSourceId())
                    .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + job.getDataSourceId()));
            StorageConfig storage = storageConfigRepository.findById(job.getStorageId())
                    .orElseThrow(() -> new IllegalArgumentException("Storage config not found: " + job.getStorageId()));
            record.setStorageId(storage.getId());

            File backupFile = buildBackupFile(source, job);
            BackupCommandFactory.PreparedCommand dumpCommand = commandFactory.buildDump(source, job, backupFile);
            commandLog.append("$ ").append(maskCommand(dumpCommand.getCommand())).append(System.lineSeparator());
            CommandResult dumpResult = commandRunner.run(dumpCommand.getCommand(), dumpCommand.getEnvironment(), new File(properties.getWorkDir()));
            commandLog.append(dumpResult.getOutput()).append(System.lineSeparator());
            if (dumpResult.getExitCode() != 0) {
                throw new IllegalStateException("Dump command failed, exitCode=" + dumpResult.getExitCode());
            }

            File finalFile = backupFile;
            if (Boolean.TRUE.equals(job.getGzipEnabled())) {
                finalFile = gzip(backupFile, commandLog);
            }

            String remotePath = "";
            if (Boolean.TRUE.equals(job.getUploadEnabled())) {
                RcloneConfigService.PreparedRclone prepared = rcloneConfigService.prepare(storage);
                try {
                    remotePath = rcloneConfigService.remoteFile(prepared.getRemoteName(), storage, finalFile.getName());
                    UploadOutcome outcome = resumableUploadService.upload(finalFile, storage, remotePath, commandLog);
                    applyUploadOutcome(record, outcome);
                } finally {
                    prepared.cleanup();
                }
            } else {
                remotePath = rcloneConfigService.remoteFile(rcloneRemoteName(storage), storage, finalFile.getName());
            }

            record.setStatus(ExecutionStatus.SUCCESS);
            record.setMessage("Backup completed");
            record.setLocalFile(finalFile.getAbsolutePath());
            record.setRemotePath(remotePath);
            record.setFileSize(finalFile.length());
        } catch (Exception e) {
            record.setStatus(ExecutionStatus.FAILED);
            record.setMessage(e.getMessage());
        } finally {
            LocalDateTime finishedAt = LocalDateTime.now();
            record.setFinishedAt(finishedAt);
            record.setDurationMillis(Duration.between(record.getStartedAt(), finishedAt).toMillis());
            record.setCommandLog(commandLog.toString());
            executionRecordRepository.save(record);

            job.setLastRunAt(record.getStartedAt());
            backupJobRepository.save(job);
        }
        return record;
    }

    private File buildBackupFile(DataSourceConfig source, BackupJob job) {
        String fileName = source.getName() + "-" + source.getDatabaseName() + "-" + job.getId() + "-" + DateTimes.fileTimestamp() + ".sql";
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return new File(properties.getBackupDir(), fileName);
    }

    private File gzip(File inputFile, StringBuilder commandLog) {
        List<String> command = new ArrayList<String>();
        command.add("gzip");
        command.add("-f");
        command.add(inputFile.getAbsolutePath());
        commandLog.append("$ ").append(maskCommand(command)).append(System.lineSeparator());
        CommandResult result = commandRunner.run(command, Collections.<String, String>emptyMap(), new File(properties.getWorkDir()));
        commandLog.append(result.getOutput()).append(System.lineSeparator());
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("gzip failed, exitCode=" + result.getExitCode());
        }
        return new File(inputFile.getAbsolutePath() + ".gz");
    }

    private String maskCommand(List<String> command) {
        return StringUtils.join(command, " ");
    }

    private String rcloneRemoteName(StorageConfig storage) {
        if (StringUtils.isNotBlank(storage.getRcloneRemote())) {
            return storage.getRcloneRemote().trim();
        }
        String id = storage.getId() == null ? "draft" : String.valueOf(storage.getId());
        return "dataark-" + storage.getType().name().toLowerCase().replace('_', '-') + "-" + id;
    }

    private void applyUploadOutcome(ExecutionRecord record, UploadOutcome outcome) {
        record.setRemotePath(outcome.getRemotePath());
        record.setMultipartUpload(outcome.isMultipartUpload());
        record.setTotalParts(outcome.getTotalParts());
        record.setUploadedParts(outcome.getUploadedParts());
        record.setManifestFile(outcome.getManifestFile());
    }
}
