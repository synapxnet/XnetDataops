package com.synapxnet.dataopsddvservice.controller;

import com.synapxnet.dataopsddvservice.common.Result;
import com.synapxnet.dataopsddvservice.entity.SqlScript;
import com.synapxnet.dataopsddvservice.entity.QueryHistory;
import com.synapxnet.dataopsddvservice.entity.SavedQuery;
import com.synapxnet.dataopsddvservice.service.SqlScriptService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ddv")
public class SqlScriptController {
    private final SqlScriptService service;

    public SqlScriptController(SqlScriptService service) { this.service = service; }

    @GetMapping("/scripts")
    public Result<List<SqlScript>> listScripts() { return Result.success(service.listScripts()); }

    @GetMapping("/scripts/{id}")
    public Result<SqlScript> getScript(@PathVariable("id") Long id) { return Result.success(service.getScriptById(id)); }

    @PostMapping("/scripts")
    public Result<SqlScript> createScript(@RequestBody SqlScript script) { return Result.success(service.createScript(script)); }

    @PutMapping("/scripts/{id}")
    public Result<SqlScript> updateScript(@PathVariable("id") Long id, @RequestBody SqlScript script) {
        script.setId(id);
        return Result.success(service.updateScript(script));
    }

    @DeleteMapping("/scripts/{id}")
    public Result<Void> deleteScript(@PathVariable("id") Long id) { service.deleteScript(id); return Result.success(); }

    @GetMapping("/history")
    public Result<List<QueryHistory>> getHistory(@RequestParam(required = false) Long datasourceId) {
        return Result.success(service.getHistory(datasourceId));
    }

    @PostMapping("/history")
    public Result<QueryHistory> recordHistory(@RequestBody QueryHistory history) { return Result.success(service.recordHistory(history)); }

    @GetMapping("/saved")
    public Result<List<SavedQuery>> listSaved() { return Result.success(service.listSavedQueries()); }

    @PostMapping("/saved")
    public Result<SavedQuery> createSaved(@RequestBody SavedQuery saved) { return Result.success(service.createSavedQuery(saved)); }

    @PutMapping("/saved/{id}")
    public Result<SavedQuery> updateSaved(@PathVariable("id") Long id, @RequestBody SavedQuery saved) {
        saved.setId(id);
        return Result.success(service.updateSavedQuery(saved));
    }

    @DeleteMapping("/saved/{id}")
    public Result<Void> deleteSaved(@PathVariable("id") Long id) { service.deleteSavedQuery(id); return Result.success(); }
}
