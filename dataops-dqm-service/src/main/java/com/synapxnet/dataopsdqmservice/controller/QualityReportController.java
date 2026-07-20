package com.synapxnet.dataopsdqmservice.controller;

import com.synapxnet.dataopsdqmservice.common.Result;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.entity.QualityAlert;
import com.synapxnet.dataopsdqmservice.service.QualityRuleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dqm")
public class QualityReportController {
    private final QualityRuleService service;

    public QualityReportController(QualityRuleService service) { this.service = service; }

    @GetMapping("/reports")
    public Result<List<QualityReport>> listReports(@RequestParam(required = false) Long ruleId) {
        return Result.success(service.listReports(ruleId));
    }

    @GetMapping("/alerts")
    public Result<List<QualityAlert>> listAlerts(@RequestParam(required = false) String status) {
        return Result.success(service.listAlerts(status));
    }

    @PutMapping("/alerts/{id}/resolve")
    public Result<Void> resolveAlert(@PathVariable("id") Long id) { service.resolveAlert(id); return Result.success(); }

    @PutMapping("/alerts/{id}/acknowledge")
    public Result<Void> acknowledgeAlert(@PathVariable("id") Long id) { service.acknowledgeAlert(id); return Result.success(); }
}
