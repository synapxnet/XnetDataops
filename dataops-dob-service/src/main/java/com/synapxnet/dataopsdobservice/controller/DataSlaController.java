package com.synapxnet.dataopsdobservice.controller;

import com.synapxnet.dataopsdobservice.common.Result;
import com.synapxnet.dataopsdobservice.entity.DataSla;
import com.synapxnet.dataopsdobservice.service.DataMonitorService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dob/slas")
public class DataSlaController {
    private final DataMonitorService service;

    public DataSlaController(DataMonitorService service) { this.service = service; }

    @GetMapping
    public Result<List<DataSla>> list() { return Result.success(service.listSlas()); }

    @PostMapping
    public Result<DataSla> create(@RequestBody DataSla sla) { return Result.success(service.createSla(sla)); }

    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats() { return Result.success(service.getSlaStats()); }
}
