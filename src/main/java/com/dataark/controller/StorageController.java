package com.dataark.controller;

import com.dataark.model.StorageConfig;
import com.dataark.repository.StorageConfigRepository;
import com.dataark.dto.TestResult;
import com.dataark.service.ConnectionTestService;
import org.apache.commons.lang3.StringUtils;
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
@RequestMapping("/api/storages")
public class StorageController {
    private final StorageConfigRepository repository;
    private final ConnectionTestService connectionTestService;

    public StorageController(StorageConfigRepository repository, ConnectionTestService connectionTestService) {
        this.repository = repository;
        this.connectionTestService = connectionTestService;
    }

    @GetMapping
    public List<StorageConfig> list() {
        return repository.findAll();
    }

    @PostMapping
    public StorageConfig create(@Valid @RequestBody StorageConfig body) {
        body.setId(null);
        fillInternalRemote(body);
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        return repository.save(body);
    }

    @PutMapping("/{id}")
    public StorageConfig update(@PathVariable Long id, @Valid @RequestBody StorageConfig body) {
        StorageConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage config not found: " + id));
        entity.setName(body.getName());
        entity.setType(body.getType());
        entity.setRcloneRemote(StringUtils.defaultIfBlank(body.getRcloneRemote(), entity.getRcloneRemote()));
        entity.setBucket(body.getBucket());
        entity.setAccessKey(body.getAccessKey());
        entity.setSecretKey(body.getSecretKey());
        entity.setRegion(body.getRegion());
        entity.setEndpoint(body.getEndpoint());
        entity.setVendor(body.getVendor());
        entity.setWebdavUrl(body.getWebdavUrl());
        entity.setWebdavUsername(body.getWebdavUsername());
        entity.setWebdavPassword(body.getWebdavPassword());
        entity.setMultipartThresholdMb(body.getMultipartThresholdMb());
        entity.setMultipartChunkMb(body.getMultipartChunkMb());
        entity.setUploadRetries(body.getUploadRetries());
        entity.setBasePath(body.getBasePath());
        entity.setConfigJson(body.getConfigJson());
        entity.setUpdatedAt(LocalDateTime.now());
        fillInternalRemote(entity);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/{id}/test")
    public TestResult test(@PathVariable Long id) {
        StorageConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage config not found: " + id));
        return connectionTestService.testStorageList(entity);
    }

    @PostMapping("/{id}/upload-test")
    public TestResult uploadTest(@PathVariable Long id) {
        StorageConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage config not found: " + id));
        return connectionTestService.testStorageUpload(entity);
    }

    @PostMapping("/test")
    public TestResult testDraft(@Valid @RequestBody StorageConfig body) {
        fillInternalRemote(body);
        return connectionTestService.testStorageList(body);
    }

    @PostMapping("/upload-test")
    public TestResult uploadTestDraft(@Valid @RequestBody StorageConfig body) {
        fillInternalRemote(body);
        return connectionTestService.testStorageUpload(body);
    }

    private void fillInternalRemote(StorageConfig body) {
        if (StringUtils.isBlank(body.getRcloneRemote())) {
            String type = body.getType() == null ? "storage" : body.getType().name().toLowerCase().replace('_', '-');
            body.setRcloneRemote("dataark-" + type + "-" + System.currentTimeMillis());
        }
    }
}
