/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：提供网页用户只读工作台接口。Purpose: Expose the browser-authenticated read-only workbench.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.governance;

import com.synapxnet.dataopsdgvservice.common.Result;
import com.synapxnet.goai.contract.DataOpsOrganizationVerifier;
import com.synapxnet.goai.contract.AgentContractException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dgv/governance")
public class GovernanceWorkbenchController {
    private final DataOpsOrganizationVerifier verifier;
    private final GovernanceWorkbenchService service;

    /** 注入身份验证和读服务。 Inject the browser authority verifier and read-only workbench service. */
    public GovernanceWorkbenchController(DataOpsOrganizationVerifier verifier, GovernanceWorkbenchService service) {
        this.verifier = verifier; this.service = service;
    }

    /** 先验证身份再取得授权资料。 Verify identity before reading any granted domain resources. */
    @GetMapping("/workbench")
    public Result<GovernanceWorkbenchService.Workbench> read(
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestHeader(value="X-Tenant-Uid", required=false) String tenant,
            @RequestHeader(value="X-Dept-Uid", required=false) String department,
            @RequestHeader(value="X-Team-Uid", required=false) String team,
            @RequestParam(required=false) String assetUid, @RequestParam(required=false) String search,
            @RequestParam(defaultValue="both") String direction, @RequestParam(defaultValue="2") int maxDepth) {
        return Result.success(service.read(verifier.verify(authorization, tenant, department, team), assetUid, search, direction, maxDepth));
    }

    /** 网页错误沿用Result包络且保留HTTP状态。 Preserve browser Result errors and their HTTP status separately from ToolResponse. */
    @ExceptionHandler(AgentContractException.class)
    public ResponseEntity<Result<Void>> handleScopeError(AgentContractException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(Result.error(exception.getHttpStatus(), exception.getMessage()));
    }
}
