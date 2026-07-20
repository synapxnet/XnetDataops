package com.synapxnet.dataopsdgvservice.controller;

import com.synapxnet.dataopsdgvservice.common.Result;
import com.synapxnet.dataopsdgvservice.entity.*;
import com.synapxnet.dataopsdgvservice.service.MetaTableService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dgv")
public class MetaTableController {
    private final MetaTableService service;

    public MetaTableController(MetaTableService service) { this.service = service; }

    @GetMapping("/tables")
    public Result<List<MetaTable>> listTables(@RequestParam(required = false) Long datasourceId) {
        return Result.success(service.listTables(datasourceId));
    }

    @GetMapping("/tables/{id}")
    public Result<MetaTable> getTable(@PathVariable("id") Long id) { return Result.success(service.getTableById(id)); }

    @PostMapping("/tables")
    public Result<MetaTable> createTable(@RequestBody MetaTable table) { return Result.success(service.createTable(table)); }

    @PutMapping("/tables/{id}")
    public Result<MetaTable> updateTable(@PathVariable("id") Long id, @RequestBody MetaTable table) {
        table.setId(id);
        return Result.success(service.updateTable(table));
    }

    @DeleteMapping("/tables/{id}")
    public Result<Void> deleteTable(@PathVariable("id") Long id) { service.deleteTable(id); return Result.success(); }

    @GetMapping("/tables/{id}/columns")
    public Result<List<MetaColumn>> getColumns(@PathVariable("id") Long id) { return Result.success(service.getColumns(id)); }

    @PutMapping("/tables/{id}/columns")
    public Result<Void> saveColumns(@PathVariable("id") Long id, @RequestBody List<MetaColumn> columns) {
        service.saveColumns(id, columns);
        return Result.success();
    }

    @GetMapping("/tables/{id}/tags")
    public Result<List<DataTag>> getTableTags(@PathVariable("id") Long id) { return Result.success(service.getTableTags(id)); }

    @PostMapping("/tables/{tableId}/tags/{tagId}")
    public Result<Void> addTableTag(@PathVariable("tableId") Long tableId, @PathVariable("tagId") Long tagId) {
        service.addTableTag(tableId, tagId);
        return Result.success();
    }

    @DeleteMapping("/tables/{tableId}/tags/{tagId}")
    public Result<Void> removeTableTag(@PathVariable("tableId") Long tableId, @PathVariable("tagId") Long tagId) {
        service.removeTableTag(tableId, tagId);
        return Result.success();
    }
}
