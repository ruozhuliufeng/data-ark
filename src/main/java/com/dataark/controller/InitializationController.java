package com.dataark.controller;

import com.dataark.dto.InitializeRequest;
import com.dataark.dto.TestResult;
import com.dataark.service.InitializationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/init")
public class InitializationController {
    private final InitializationService initializationService;

    public InitializationController(InitializationService initializationService) {
        this.initializationService = initializationService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("initialized", initializationService.isInitialized());
        return map;
    }

    @PostMapping
    public TestResult initialize(@Valid @RequestBody InitializeRequest request) {
        if (initializationService.isInitialized()) {
            return TestResult.fail("DataArk 已完成初始化", "如需重新初始化，请备份后删除配置文件并重启。");
        }
        return initializationService.initialize(request);
    }
}
