package com.synapxnet.dataopsdmsservice.controller;

import com.synapxnet.dataopsdmsservice.common.Result;
import com.synapxnet.dataopsdmsservice.entity.MaskingPolicy;
import com.synapxnet.dataopsdmsservice.service.MaskingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dms/policies")
public class MaskingPolicyController {
    private final MaskingService service;

    public MaskingPolicyController(MaskingService service) { this.service = service; }

    @GetMapping
    public Result<List<MaskingPolicy>> list(@RequestParam(required = false) Long datasourceId) {
        return Result.success(service.listPolicies(datasourceId));
    }

    @GetMapping("/{id}")
    public Result<MaskingPolicy> get(@PathVariable("id") Long id) { return Result.success(service.getPolicyById(id)); }

    @PostMapping
    public Result<MaskingPolicy> create(@RequestBody MaskingPolicy policy) { return Result.success(service.createPolicy(policy)); }

    @PutMapping("/{id}")
    public Result<MaskingPolicy> update(@PathVariable("id") Long id, @RequestBody MaskingPolicy policy) {
        policy.setId(id);
        return Result.success(service.updatePolicy(policy));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { service.deletePolicy(id); return Result.success(); }

    @PostMapping("/{id}/toggle")
    public Result<MaskingPolicy> toggle(@PathVariable("id") Long id) { return Result.success(service.togglePolicy(id)); }
}
