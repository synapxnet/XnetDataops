/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：恢复并约束原生只读取证契约。Purpose: Restore bounded native evidence contracts.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
Historical SynapXnet source restored selectively; existing repository license retained.
 */
package com.synapxnet.dataopsdqmservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.DataOpsResourceScopes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 DataOps 质量报告只读证据工具。
 * English: Read or verify persisted quality evidence; preserve absent data and disclose malformed details.
 */
@RestController
public class AgentQualityToolController {

    private static final String TOOL_NAME = "dataops.quality.report.get";
    private final QualityEvidenceService evidenceService;
    private final DataOpsResourceScopes scopes;

    /**
     * 创建质量报告工具 Controller。
     *
     * @param evidenceService 质量证据领域服务
 * English: Inject the quality evidence service and explicit resource scope repository.
 */
    public AgentQualityToolController(QualityEvidenceService evidenceService, DataOpsResourceScopes scopes) {
        this.evidenceService = evidenceService;
        this.scopes = scopes;
    }

    /**
     * 获取真实数据库中的质量报告、Schema 关联和契约检查事实。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 质量报告证据
 * English: Authorize the report and its asset binding before returning persisted quality and contract evidence.
 */
    @PostMapping("/api/agent/v1/tools/dataops.quality.report.get:invoke")
    public AgentContract.ToolResponse<QualityEvidenceService.QualityReportEvidence> invoke(
            @RequestBody AgentContract.ToolRequest<QualityReportArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, TOOL_NAME, body);
        if (body.arguments() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        DataOpsResourceScopes.Scope scope = scopes.forWorkspace(context.workspaceId());
        scope.requireReport(body.arguments().reportUid());
        QualityEvidenceService.QualityReportEvidence evidence =
                evidenceService.getReport(body.arguments().reportUid());
        scope.requireAsset(evidence.assetUid());
        return AgentContract.success(evidence, context, "XnetDataops/dqm/" + scope.executionMode(), null, startedNanos);
    }

    /** 表示质量报告查询参数。
 * English: Read or verify persisted quality evidence; preserve absent data and disclose malformed details.
 */
    public record QualityReportArguments(String reportUid) {
    }
}
