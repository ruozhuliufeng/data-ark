package com.dataark.controller;

import com.dataark.model.BackupJob;
import com.dataark.repository.BackupJobRepository;
import com.dataark.service.BackupExecutionService;
import com.dataark.service.BackupSchedulerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class BackupJobController {
    private final BackupJobRepository repository;
    private final BackupSchedulerService schedulerService;
    private final BackupExecutionService executionService;

    public BackupJobController(BackupJobRepository repository,
                               BackupSchedulerService schedulerService,
                               BackupExecutionService executionService) {
        this.repository = repository;
        this.schedulerService = schedulerService;
        this.executionService = executionService;
    }

    @GetMapping
    public List<BackupJob> list() {
        return repository.findAll();
    }

    @PostMapping
    public BackupJob create(@Valid @RequestBody BackupJob body) {
        body.setId(null);
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        BackupJob saved = repository.save(body);
        schedulerService.reschedule(saved);
        return saved;
    }

    @PutMapping("/{id}")
    public BackupJob update(@PathVariable Long id, @Valid @RequestBody BackupJob body) {
        BackupJob entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Backup job not found: " + id));
        entity.setName(body.getName());
        entity.setDataSourceId(body.getDataSourceId());
        entity.setStorageId(body.getStorageId());
        entity.setCronExpression(body.getCronExpression());
        entity.setIncludeTables(body.getIncludeTables());
        entity.setExcludeTables(body.getExcludeTables());
        entity.setGzipEnabled(body.getGzipEnabled());
        entity.setUploadEnabled(body.getUploadEnabled());
        entity.setRetentionDays(body.getRetentionDays());
        entity.setStatus(body.getStatus());
        entity.setUpdatedAt(LocalDateTime.now());
        BackupJob saved = repository.save(entity);
        schedulerService.reschedule(saved);
        return saved;
    }

    @PostMapping("/{id}/run")
    public void run(@PathVariable Long id) {
        executionService.runAsync(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        schedulerService.cancel(id);
        repository.deleteById(id);
    }
}
