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
package com.synapxnet.dataopsdgvservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.DataOpsResourceScopes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 暴露 DataOps Schema 快照和血缘只读证据工具。
 * English: Read and project persisted lineage with explicit direction, bounded traversal and stable asset identities.
 */
@RestController
public class AgentDgvToolController {
    // 仅启用的真实运行时分流，缺失响应不得回退。 Route only enabled real execution; never fall back on missing evidence.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FeatureDriftReadOnlyClient featureDriftRuntime;


    private final AgentDgvEvidenceService evidenceService;
    private final DataOpsResourceScopes scopes;

    /** 保留只读工具错误码与上下文，避免旧全局处理器把权限拒绝变成500。 / Preserve read-tool error codes and context instead of converting denials to generic 500 errors. */
    @org.springframework.web.bind.annotation.ExceptionHandler(AgentContractException.class)
    public org.springframework.http.ResponseEntity<AgentContract.ToolResponse<Void>> contractFailure(
            AgentContractException error, HttpServletRequest request) {
        Object value = request.getAttribute(AgentContract.CONTEXT_ATTRIBUTE);
        AgentContract.RequestContext context = value instanceof AgentContract.RequestContext trusted ? trusted : null;
        AgentContract.ToolMeta meta = new AgentContract.ToolMeta(
                context == null ? null : context.requestId(), context == null ? null : context.workspaceId(),
                context == null ? null : context.incidentId(), context == null ? null : context.traceId(),
                context == null ? null : context.toolName(), AgentContract.CONTRACT_VERSION, Instant.now(), 0L,
                "XnetDataops/dgv", null, null);
        AgentContract.ToolError failure = new AgentContract.ToolError(error.getCode(), error.getMessage(), error.isRetryable(), error.getDetails());
        return org.springframework.http.ResponseEntity.status(error.getHttpStatus())
                .body(new AgentContract.ToolResponse<>(false, null, failure, meta, null));
    }

    /**
     * 创建 DGV Agent 工具 Controller。
     *
     * @param evidenceService DGV 证据领域服务
 * English: Inject the DGV evidence service and explicit resource scope repository.
 */
    public AgentDgvToolController(AgentDgvEvidenceService evidenceService, DataOpsResourceScopes scopes) {
        this.evidenceService = evidenceService;
        this.scopes = scopes;
    }

    /**
     * 获取指定资产的持久化 Schema 快照。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return Schema 快照证据
 * English: Authorize the requested asset and return its persisted schema snapshot in the Agent envelope.
 */
    @PostMapping("/api/agent/v1/tools/dataops.schema.snapshot.get:invoke")
    public AgentContract.ToolResponse<AgentDgvEvidenceService.SchemaSnapshotEvidence> schemaSnapshot(
            @RequestBody AgentContract.ToolRequest<SchemaSnapshotArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "dataops.schema.snapshot.get";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        SchemaSnapshotArguments arguments = body.arguments();
        if (arguments == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        scopes.forWorkspace(context.workspaceId()).requireAsset(arguments.assetUid());
        AgentDgvEvidenceService.SchemaSnapshotEvidence evidence = evidenceService.getSchemaSnapshot(
                arguments.assetUid(), arguments.schemaVersion(), arguments.observedAt());
        return AgentContract.success(
                evidence, context, "XnetDataops/dgv/" + scopes.forWorkspace(context.workspaceId()).executionMode(), evidence.schemaVersion(), startedNanos);
    }

    /**
     * 获取指定资产的有界血缘证据。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 稳定排序的血缘证据
 * English: Authorize the root asset and return lineage restricted to the same verified resource scope.
 */
    @PostMapping("/api/agent/v1/tools/dataops.lineage.get:invoke")
    public AgentContract.ToolResponse<AgentDgvEvidenceService.LineageEvidence> lineage(
            @RequestBody AgentContract.ToolRequest<LineageArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "dataops.lineage.get";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        LineageArguments arguments = requireLineageArguments(body.arguments());
        DataOpsResourceScopes.Scope scope = scopes.forWorkspace(context.workspaceId());
        scope.requireAsset(arguments.assetUid());
        AgentDgvEvidenceService.LineageEvidence evidence = evidenceService.getLineage(
                arguments.assetUid(), arguments.direction(), arguments.depth(), scope::allowsAsset);
        return AgentContract.success(evidence, context, "XnetDataops/dgv/" + scope.executionMode(), null, startedNanos);
    }

    /**
     * 校验血缘查询参数。
     *
     * @param arguments 血缘查询参数
     * @return 已校验参数
 * English: Require a nonnull arguments object; the evidence service validates its individual fields.
 */
    private LineageArguments requireLineageArguments(LineageArguments arguments) {
        if (arguments == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        return arguments;
    }

    /** 表示 Schema 快照查询参数。
 * English: Read or verify typed schema evidence from persisted snapshots while excluding sample values.
 */
    public record SchemaSnapshotArguments(String assetUid, String schemaVersion, Instant observedAt) {
    }

    /** 表示血缘查询参数。
 * English: Read and project persisted lineage with explicit direction, bounded traversal and stable asset identities.
 */
    public record LineageArguments(
            String assetUid,
            AgentDgvEvidenceService.Direction direction,
            Integer depth) {
    }
}
