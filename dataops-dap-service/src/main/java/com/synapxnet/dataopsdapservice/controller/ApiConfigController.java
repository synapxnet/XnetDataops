package com.synapxnet.dataopsdapservice.controller;

import com.synapxnet.dataopsdapservice.common.Result;
import com.synapxnet.dataopsdapservice.entity.ApiConfig;
import com.synapxnet.dataopsdapservice.service.ApiConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dap")
public class ApiConfigController {

    private final ApiConfigService apiConfigService;

    public ApiConfigController(ApiConfigService apiConfigService) {
        this.apiConfigService = apiConfigService;
    }

    @GetMapping("/configs")
    public Result<List<ApiConfig>> list() {
        return Result.success(apiConfigService.listAllConfigs());
    }

    @GetMapping("/configs/{id}")
    public Result<ApiConfig> get(@PathVariable("id") Long id) {
        return Result.success(apiConfigService.getConfigById(id));
    }

    @PostMapping("/configs")
    public Result<ApiConfig> create(@RequestBody ApiConfig apiConfig) {
        return Result.success(apiConfigService.createConfig(apiConfig));
    }

    @PutMapping("/configs/{id}")
    public Result<ApiConfig> update(@PathVariable("id") Long id, @RequestBody ApiConfig apiConfig) {
        apiConfig.setId(id);
        return Result.success(apiConfigService.updateConfig(apiConfig));
    }

    @DeleteMapping("/configs/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        apiConfigService.deleteConfig(id);
        return Result.success();
    }

    @PostMapping("/configs/{id}/publish")
    public Result<ApiConfig> publish(@PathVariable("id") Long id) {
        return Result.success(apiConfigService.publishConfig(id));
    }

    @PostMapping("/configs/{id}/deprecate")
    public Result<ApiConfig> deprecate(@PathVariable("id") Long id) {
        return Result.success(apiConfigService.deprecateConfig(id));
    }
}
