package com.dataark.controller;

import com.dataark.model.DataSourceConfig;
import com.dataark.repository.DataSourceConfigRepository;
import com.dataark.service.ConnectionTestService;
import com.dataark.dto.TestResult;
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
@RequestMapping("/api/datasources")
public class DataSourceController {
    private final DataSourceConfigRepository repository;
    private final ConnectionTestService connectionTestService;

    public DataSourceController(DataSourceConfigRepository repository, ConnectionTestService connectionTestService) {
        this.repository = repository;
        this.connectionTestService = connectionTestService;
    }

    @GetMapping
    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    @PostMapping
    public DataSourceConfig create(@Valid @RequestBody DataSourceConfig body) {
        body.setId(null);
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        return repository.save(body);
    }

    @PutMapping("/{id}")
    public DataSourceConfig update(@PathVariable Long id, @Valid @RequestBody DataSourceConfig body) {
        DataSourceConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));
        entity.setName(body.getName());
        entity.setType(body.getType());
        entity.setHost(body.getHost());
        entity.setPort(body.getPort());
        entity.setUsername(body.getUsername());
        entity.setPassword(body.getPassword());
        entity.setDatabaseName(body.getDatabaseName());
        entity.setOptionsJson(body.getOptionsJson());
        entity.setUpdatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/{id}/test")
    public TestResult test(@PathVariable Long id) {
        DataSourceConfig entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));
        return connectionTestService.testDataSource(entity);
    }

    @PostMapping("/test")
    public TestResult testDraft(@Valid @RequestBody DataSourceConfig body) {
        return connectionTestService.testDataSource(body);
    }
}
