package com.synapxnet.dataopsdauservice.controller;

import com.synapxnet.dataopsdauservice.common.Result;
import com.synapxnet.dataopsdauservice.entity.AuditLog;
import com.synapxnet.dataopsdauservice.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dau/audit-logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Result<List<AuditLog>> list(@RequestParam(required = false) String module,
                                       @RequestParam(required = false) String action,
                                       @RequestParam(required = false) Long userId) {
        return Result.success(auditService.listLogs(module, action, userId));
    }

    @PostMapping
    public Result<AuditLog> create(@RequestBody AuditLog auditLog) {
        return Result.success(auditService.createLog(auditLog));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(auditService.getLogStats());
    }
}
