package com.synapxnet.dataopsdsmservice.controller;

import com.synapxnet.dataopsdsmservice.common.Result;
import com.synapxnet.dataopsdsmservice.entity.DataSource;
import com.synapxnet.dataopsdsmservice.service.DataSourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dsm/datasources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public Result<List<DataSource>> list(@RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return Result.success(dataSourceService.listByType(type));
        }
        return Result.success(dataSourceService.listAll());
    }

    @GetMapping("/{id}")
    public Result<DataSource> get(@PathVariable("id") Long id) {
        return Result.success(dataSourceService.getById(id));
    }

    @PostMapping
    public Result<DataSource> create(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.create(dataSource));
    }

    @PutMapping("/{id}")
    public Result<DataSource> update(@PathVariable("id") Long id, @RequestBody DataSource dataSource) {
        dataSource.setId(id);
        return Result.success(dataSourceService.update(dataSource));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        dataSourceService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(@PathVariable("id") Long id) {
        boolean success = dataSourceService.testConnection(id);
        return Result.success(Map.of("success", success));
    }
}
