package com.yuyu.workflow.controller;

import com.yuyu.workflow.common.Resp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "健康检查")
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 返回服务健康状态。
     */
    @Operation(summary = "健康检查", description = "返回服务运行状态")
    @GetMapping("/ping")
    public Resp<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("service", "approval-workflow");
        data.put("time", LocalDateTime.now());
        return Resp.success(data);
    }
}
