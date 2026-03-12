package com.synapxnet.dataopsdmsservice.controller;

import com.synapxnet.dataopsdmsservice.common.Result;
import com.synapxnet.dataopsdmsservice.entity.MaskingRule;
import com.synapxnet.dataopsdmsservice.service.MaskingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dms/rules")
public class MaskingRuleController {
    private final MaskingService service;

    public MaskingRuleController(MaskingService service) { this.service = service; }

    @GetMapping
    public Result<List<MaskingRule>> list() { return Result.success(service.listRules()); }

    @GetMapping("/{id}")
    public Result<MaskingRule> get(@PathVariable("id") Long id) { return Result.success(service.getRuleById(id)); }

    @PostMapping
    public Result<MaskingRule> create(@RequestBody MaskingRule rule) { return Result.success(service.createRule(rule)); }

    @PutMapping("/{id}")
    public Result<MaskingRule> update(@PathVariable("id") Long id, @RequestBody MaskingRule rule) {
        rule.setId(id);
        return Result.success(service.updateRule(rule));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deleteRule(id); return Result.success(); }
}
