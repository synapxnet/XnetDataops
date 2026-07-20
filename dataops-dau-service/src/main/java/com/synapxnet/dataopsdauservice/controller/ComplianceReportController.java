package com.synapxnet.dataopsdauservice.controller;

import com.synapxnet.dataopsdauservice.common.Result;
import com.synapxnet.dataopsdauservice.entity.ComplianceReport;
import com.synapxnet.dataopsdauservice.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dau/compliance-reports")
public class ComplianceReportController {

    private final AuditService auditService;

    public ComplianceReportController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Result<List<ComplianceReport>> list() {
        return Result.success(auditService.listReports());
    }

    @PostMapping
    public Result<ComplianceReport> generate(@RequestBody ComplianceReport report) {
        return Result.success(auditService.generateReport(report));
    }

    @GetMapping("/{id}")
    public Result<ComplianceReport> get(@PathVariable("id") Long id) {
        return Result.success(auditService.getReportById(id));
    }

    @PutMapping("/{id}/review")
    public Result<ComplianceReport> review(@PathVariable("id") Long id) {
        return Result.success(auditService.reviewReport(id));
    }

    @PutMapping("/{id}/archive")
    public Result<ComplianceReport> archive(@PathVariable("id") Long id) {
        return Result.success(auditService.archiveReport(id));
    }
}
