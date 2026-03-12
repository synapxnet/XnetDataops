package com.synapxnet.dataopsdgvservice.controller;

import com.synapxnet.dataopsdgvservice.common.Result;
import com.synapxnet.dataopsdgvservice.entity.DataTag;
import com.synapxnet.dataopsdgvservice.service.MetaTableService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dgv/tags")
public class DataTagController {
    private final MetaTableService service;

    public DataTagController(MetaTableService service) { this.service = service; }

    @GetMapping
    public Result<List<DataTag>> list() { return Result.success(service.listTags()); }

    @PostMapping
    public Result<DataTag> create(@RequestBody DataTag tag) { return Result.success(service.createTag(tag)); }

    @PutMapping("/{id}")
    public Result<DataTag> update(@PathVariable("id") Long id, @RequestBody DataTag tag) {
        tag.setId(id);
        return Result.success(service.updateTag(tag));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deleteTag(id); return Result.success(); }
}
