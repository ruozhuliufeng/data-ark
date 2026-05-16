package com.dataark.controller;

import com.dataark.model.DatabaseType;
import com.dataark.model.JobStatus;
import com.dataark.model.MemberRole;
import com.dataark.model.StorageType;
import com.dataark.service.InitializationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaController {
    private final InitializationService initializationService;

    public MetaController(InitializationService initializationService) {
        this.initializationService = initializationService;
    }

    @GetMapping
    public Map<String, Object> meta() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("initialized", initializationService.isInitialized());
        map.put("databaseTypes", DatabaseType.values());
        map.put("storageTypes", StorageType.values());
        map.put("jobStatuses", JobStatus.values());
        map.put("memberRoles", MemberRole.values());
        return map;
    }
}
