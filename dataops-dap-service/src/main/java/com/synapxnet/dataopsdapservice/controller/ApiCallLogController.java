package com.synapxnet.dataopsdapservice.controller;

import com.synapxnet.dataopsdapservice.common.Result;
import com.synapxnet.dataopsdapservice.entity.ApiCallLog;
import com.synapxnet.dataopsdapservice.service.ApiConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dap/logs")
public class ApiCallLogController {

    private final ApiConfigService apiConfigService;

    public ApiCallLogController(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
    }

    @GetMapping
    public Result<List<ApiCallLog>> list(@RequestParam(required = false) Long apiConfigId) {
        return Result.success(apiConfigService.listLogs(apiConfigId));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Long apiConfigId) {
        return Result.success(apiConfigService.getCallStats(apiConfigId));
    }
}
