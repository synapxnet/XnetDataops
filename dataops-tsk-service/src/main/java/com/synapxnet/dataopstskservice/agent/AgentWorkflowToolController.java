/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 治理执行与只读取证 / Governed execution and read-only evidence.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17
 * Version: 1.3.0 | Security Level: INTERNAL
 * __version__: 1.3.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopstskservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 DataOps 工作流实例只读证据工具。
 */
@RestController
public class AgentWorkflowToolController {

    private static final String TOOL_NAME = "dataops.workflow.instance.get";
    private final WorkflowInstanceEvidenceService evidenceService;
    private final CompetitionDatasetToolController datasetTools;

    /**
     * 注入工作流证据和唯一数据版本源。 / Inject workflow evidence and the shared dataset version source.
     *
     * @param evidenceService 任务实例证据领域服务
     */
    public AgentWorkflowToolController(WorkflowInstanceEvidenceService evidenceService, CompetitionDatasetToolController datasetTools) {
        this.evidenceService = evidenceService;
        this.datasetTools = datasetTools;
    }

    /**
     * 获取任务证据和当前关联资源版本。 / Read workflow evidence and current associated resource versions.
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 工作流实例证据
     */
    @PostMapping("/api/agent/v1/tools/dataops.workflow.instance.get:invoke")
    public AgentContract.ToolResponse<WorkflowInstanceEvidenceService.WorkflowInstanceEvidence> invoke(
            @RequestBody AgentContract.ToolRequest<WorkflowInstanceArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, TOOL_NAME, body);
        if (body.arguments() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        return datasetTools.readConsistently(() -> {
            WorkflowInstanceEvidenceService.WorkflowInstanceEvidence evidence = evidenceService.get(
                    body.arguments().instanceUid(), Boolean.TRUE.equals(body.arguments().includeLogSummary()));
            java.util.Map<String, String> versions = datasetTools.workflowResourceVersions(context.workspaceId(), evidence.instanceUid());
            return AgentContract.success(evidence.withResourceVersions(versions), context, "XnetDataops/tsk", null, startedNanos);
        });
    }

    /** 表示工作流实例查询参数。
     * English: Workflow Instance Arguments.
     */
    public record WorkflowInstanceArguments(String instanceUid, Boolean includeLogSummary) {
    }
}
