package com.synapxnet.dataopsdqmservice.controller;

import com.synapxnet.dataopsdqmservice.common.Result;
import com.synapxnet.dataopsdqmservice.entity.QualityRule;
import com.synapxnet.dataopsdqmservice.service.QualityRuleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dqm/rules")
public class QualityRuleController {
    private final QualityRuleService service;

    public QualityRuleController(QualityRuleService service) { this.service = service; }

    @GetMapping
    public Result<List<QualityRule>> list() { return Result.success(service.listRules()); }

    @GetMapping("/{id}")
    public Result<QualityRule> get(@PathVariable("id") Long id) { return Result.success(service.getRuleById(id)); }

    @PostMapping
    public Result<QualityRule> create(@RequestBody QualityRule rule) { return Result.success(service.createRule(rule)); }

    @PutMapping("/{id}")
    public Result<QualityRule> update(@PathVariable("id") Long id, @RequestBody QualityRule rule) {
        rule.setId(id);
        return Result.success(service.updateRule(rule));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deleteRule(id); return Result.success(); }
}
