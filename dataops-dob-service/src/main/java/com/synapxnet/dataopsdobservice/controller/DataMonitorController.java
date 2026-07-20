package com.synapxnet.dataopsdobservice.controller;

import com.synapxnet.dataopsdobservice.common.Result;
import com.synapxnet.dataopsdobservice.entity.DataMonitor;
import com.synapxnet.dataopsdobservice.service.DataMonitorService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dob/monitors")
public class DataMonitorController {
    private final DataMonitorService service;

    public DataMonitorController(DataMonitorService service) { this.service = service; }

    @GetMapping
    public Result<List<DataMonitor>> list() { return Result.success(service.listMonitors()); }

    @GetMapping("/{id}")
    public Result<DataMonitor> get(@PathVariable("id") Long id) { return Result.success(service.getMonitorById(id)); }

    @PostMapping
    public Result<DataMonitor> create(@RequestBody DataMonitor monitor) { return Result.success(service.createMonitor(monitor)); }

    @PutMapping("/{id}")
    public Result<DataMonitor> update(@PathVariable("id") Long id, @RequestBody DataMonitor monitor) {
        monitor.setId(id);
        return Result.success(service.updateMonitor(monitor));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deleteMonitor(id); return Result.success(); }

    @PutMapping("/{id}/toggle")
    public Result<DataMonitor> toggle(@PathVariable("id") Long id) { return Result.success(service.toggleMonitor(id)); }
}
