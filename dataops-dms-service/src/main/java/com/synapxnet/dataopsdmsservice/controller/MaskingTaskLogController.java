package com.synapxnet.dataopsdmsservice.controller;

import com.synapxnet.dataopsdmsservice.common.Result;
import com.synapxnet.dataopsdmsservice.entity.MaskingTaskLog;
import com.synapxnet.dataopsdmsservice.service.MaskingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dms/task-logs")
public class MaskingTaskLogController {
    private final MaskingService service;

    public MaskingTaskLogController(MaskingService service) { this.service = service; }

    @GetMapping
    public Result<List<MaskingTaskLog>> list(@RequestParam(required = false) Long policyId) {
        return Result.success(service.listLogs(policyId));
    }
}
