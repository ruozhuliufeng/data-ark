package com.dataark.controller;

import com.dataark.dto.TestResult;
import com.dataark.model.ExecutionRecord;
import com.dataark.model.ExecutionStatus;
import com.dataark.model.StorageConfig;
import com.dataark.repository.ExecutionRecordRepository;
import com.dataark.repository.StorageConfigRepository;
import com.dataark.service.ResumableUploadService;
import com.dataark.service.UploadOutcome;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/executions")
public class ExecutionRecordController {
    private final ExecutionRecordRepository repository;
    private final StorageConfigRepository storageConfigRepository;
    private final ResumableUploadService resumableUploadService;

    public ExecutionRecordController(ExecutionRecordRepository repository,
                                     StorageConfigRepository storageConfigRepository,
                                     ResumableUploadService resumableUploadService) {
        this.repository = repository;
        this.storageConfigRepository = storageConfigRepository;
        this.resumableUploadService = resumableUploadService;
    }

    @GetMapping
    public List<ExecutionRecord> list() {
        return repository.findTop100ByOrderByStartedAtDesc();
    }

    @GetMapping("/job/{jobId}")
    public List<ExecutionRecord> listByJob(@PathVariable Long jobId) {
        return repository.findTop50ByJobIdOrderByStartedAtDesc(jobId);
    }

    @PostMapping("/{id}/resume-upload")
    public TestResult resumeUpload(@PathVariable Long id) {
        ExecutionRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Execution record not found: " + id));
        if (record.getStorageId() == null) {
            return TestResult.fail("无法续传", "执行记录缺少存储目标信息。");
        }
        if (record.getLocalFile() == null || !new File(record.getLocalFile()).exists()) {
            return TestResult.fail("无法续传", "本地备份文件不存在：" + record.getLocalFile());
        }
        if (record.getRemotePath() == null) {
            return TestResult.fail("无法续传", "执行记录缺少远端路径。");
        }

        StorageConfig storage = storageConfigRepository.findById(record.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("Storage config not found: " + record.getStorageId()));
        StringBuilder commandLog = new StringBuilder(record.getCommandLog() == null ? "" : record.getCommandLog());
        commandLog.append(System.lineSeparator()).append("Resume multipart upload at ")
                .append(LocalDateTime.now()).append(System.lineSeparator());

        LocalDateTime startedAt = LocalDateTime.now();
        record.setStatus(ExecutionStatus.RUNNING);
        repository.save(record);
        try {
            UploadOutcome outcome = resumableUploadService.resume(
                    new File(record.getLocalFile()),
                    storage,
                    record.getRemotePath(),
                    record.getManifestFile(),
                    commandLog);
            record.setStatus(ExecutionStatus.SUCCESS);
            record.setMessage("Resume upload completed");
            record.setMultipartUpload(outcome.isMultipartUpload());
            record.setTotalParts(outcome.getTotalParts());
            record.setUploadedParts(outcome.getUploadedParts());
            record.setManifestFile(outcome.getManifestFile());
            return TestResult.ok("续传完成", "已上传分片 " + outcome.getUploadedParts() + "/" + outcome.getTotalParts());
        } catch (Exception e) {
            record.setStatus(ExecutionStatus.FAILED);
            record.setMessage(e.getMessage());
            return TestResult.fail("续传失败", e.getMessage());
        } finally {
            LocalDateTime finishedAt = LocalDateTime.now();
            record.setFinishedAt(finishedAt);
            record.setDurationMillis(Duration.between(startedAt, finishedAt).toMillis());
            record.setCommandLog(commandLog.toString());
            repository.save(record);
        }
    }
}
