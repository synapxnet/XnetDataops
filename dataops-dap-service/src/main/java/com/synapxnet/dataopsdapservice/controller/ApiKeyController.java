package com.synapxnet.dataopsdapservice.controller;

import com.synapxnet.dataopsdapservice.common.Result;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import com.synapxnet.dataopsdapservice.service.ApiConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dap/keys")
public class ApiKeyController {

    private final ApiConfigService apiConfigService;

    public ApiKeyController(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
    }

    @GetMapping
    public Result<List<ApiKey>> list() {
        return Result.success(apiConfigService.listAllKeys());
    }

    @PostMapping
    public Result<ApiKey> create(@RequestBody ApiKey apiKey) {
        return Result.success(apiConfigService.createKey(apiKey));
    }

    @PostMapping("/{id}/revoke")
    public Result<ApiKey> revoke(@PathVariable("id") Long id) {
        return Result.success(apiConfigService.revokeKey(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        apiConfigService.deleteKey(id);
        return Result.success();
    }
}
