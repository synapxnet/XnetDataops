package com.synapxnet.dataopsdgvservice.controller;

import com.synapxnet.dataopsdgvservice.common.Result;
import com.synapxnet.dataopsdgvservice.entity.DataLineage;
import com.synapxnet.dataopsdgvservice.service.MetaTableService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dgv/lineage")
public class DataLineageController {
    private final MetaTableService service;

    public DataLineageController(MetaTableService service) { this.service = service; }

    @GetMapping
    public Result<List<DataLineage>> list(@RequestParam(required = false) Long tableId) {
        return Result.success(service.listLineage(tableId));
    }

    @PostMapping
    public Result<DataLineage> create(@RequestBody DataLineage lineage) { return Result.success(service.createLineage(lineage)); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deleteLineage(id); return Result.success(); }
}
