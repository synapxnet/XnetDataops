package com.synapxnet.dataopsdimservice.controller;

import com.synapxnet.dataopsdimservice.common.Result;
import com.synapxnet.dataopsdimservice.entity.SyncTask;
import com.synapxnet.dataopsdimservice.entity.FieldMapping;
import com.synapxnet.dataopsdimservice.entity.SyncLog;
import com.synapxnet.dataopsdimservice.service.SyncTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dim")
public class SyncTaskController {

    private final SyncTaskService syncTaskService;

    public SyncTaskController(SyncTaskService syncTaskService) {
        this.syncTaskService = syncTaskService;
    }

    @GetMapping("/tasks")
    public Result<List<SyncTask>> list() {
        return Result.success(syncTaskService.listAll());
    }

    @GetMapping("/tasks/{id}")
    public Result<SyncTask> get(@PathVariable("id") Long id) {
        return Result.success(syncTaskService.getById(id));
    }

    @PostMapping("/tasks")
    public Result<SyncTask> create(@RequestBody SyncTask task) {
        return Result.success(syncTaskService.create(task));
    }

    @PutMapping("/tasks/{id}")
    public Result<SyncTask> update(@PathVariable("id") Long id, @RequestBody SyncTask task) {
        task.setId(id);
        return Result.success(syncTaskService.update(task));
    }

    @DeleteMapping("/tasks/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        syncTaskService.delete(id);
        return Result.success();
    }

    @PutMapping("/tasks/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> params) {
        syncTaskService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    @GetMapping("/tasks/{id}/mappings")
    public Result<List<FieldMapping>> getMappings(@PathVariable("id") Long id) {
        return Result.success(syncTaskService.getMappings(id));
    }

    @PutMapping("/tasks/{id}/mappings")
    public Result<Void> saveMappings(@PathVariable("id") Long id, @RequestBody List<FieldMapping> mappings) {
        syncTaskService.saveMappings(id, mappings);
        return Result.success();
    }

    @GetMapping("/tasks/{id}/logs")
    public Result<List<SyncLog>> getLogs(@PathVariable("id") Long id) {
        return Result.success(syncTaskService.getLogs(id));
    }
}
