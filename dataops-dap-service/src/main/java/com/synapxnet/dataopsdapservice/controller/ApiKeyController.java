/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：API密钥服务端授权和安全响应。Purpose: Server authorization and safe API key responses.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopsdapservice.controller;

import com.synapxnet.dataopsdapservice.common.Result;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import com.synapxnet.dataopsdapservice.dto.ApiKeySummary;
import com.synapxnet.dataopsdapservice.security.ApiKeyAdministratorVerifier;
import com.synapxnet.dataopsdapservice.service.ApiConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dap/keys")
public class ApiKeyController {
    private final ApiConfigService apiConfigService;
    private final ApiKeyAdministratorVerifier administratorVerifier;
    /** 注入业务服务和可信管理员身份验证器。 Inject the business service and trusted administrator verifier. */
    public ApiKeyController(ApiConfigService apiConfigService, ApiKeyAdministratorVerifier administratorVerifier) {
        this.apiConfigService=apiConfigService;
        this.administratorVerifier=administratorVerifier;
    }
    /** 仅管理员可读取脱敏密钥列表。 Only administrators may read masked key summaries. */
    @GetMapping
    public Result<List<ApiKeySummary>> list(@RequestHeader(value="Authorization",required=false) String authorization) {
        administratorVerifier.requireAdministrator(authorization);
        return Result.success(apiConfigService.listAllKeys().stream().map(ApiKeySummary::from).toList());
    }
    /** 仅在已认证管理员的创建响应中返回完整凭据。 Return full credentials only in the authenticated administrator creation response. */
    @PostMapping
    public Result<ApiKey> create(@RequestHeader(value="Authorization",required=false) String authorization, @RequestBody ApiKey apiKey) {
        apiKey.setCreatedBy(administratorVerifier.requireAdministrator(authorization));
        return Result.success(apiConfigService.createKey(apiKey));
    }
    /** 授权后吊销密钥，响应仍只返回脱敏字段。 Revoke an authorized key and return only masked fields. */
    @PostMapping("/{id}/revoke")
    public Result<ApiKeySummary> revoke(@RequestHeader(value="Authorization",required=false) String authorization, @PathVariable("id") Long id) {
        administratorVerifier.requireAdministrator(authorization);
        return Result.success(ApiKeySummary.from(apiConfigService.revokeKey(id)));
    }
    /** 在服务端验证管理员后删除密钥。 Delete a key only after server-side administrator verification. */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value="Authorization",required=false) String authorization, @PathVariable("id") Long id) {
        administratorVerifier.requireAdministrator(authorization);
        apiConfigService.deleteKey(id);
        return Result.success();
    }
}
