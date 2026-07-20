package com.synapxnet.dataopsdauservice.controller;

import com.synapxnet.dataopsdauservice.common.Result;
import com.synapxnet.dataopsdauservice.entity.DataChangeRecord;
import com.synapxnet.dataopsdauservice.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dau/data-changes")
public class DataChangeController {

    private final AuditService auditService;

    public DataChangeController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Result<List<DataChangeRecord>> list(@RequestParam(required = false) Long datasourceId,
                                               @RequestParam(required = false) String tableName) {
        return Result.success(auditService.listChanges(datasourceId, tableName));
    }

    @PostMapping
    public Result<DataChangeRecord> record(@RequestBody DataChangeRecord record) {
        return Result.success(auditService.recordChange(record));
    }
}
