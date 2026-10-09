package com.dongchedi.car.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统信息接口，用于健康检查与版本核对。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final String applicationName;

    public SystemController(@Value("${spring.application.name:dongchedi}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", applicationName);
        body.put("displayName", "车帝选车 · 汽车筛选与推荐系统");
        body.put("version", "1.0.0");
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("serverTime", LocalDateTime.now());
        body.put("buildDate", LocalDate.now().toString());
        return body;
    }
}
