package com.synapxnet.dataopsdgvservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 暴露 DataOps Schema 快照和血缘只读证据工具。
 */
@RestController
public class AgentDgvToolController {

    private final AgentDgvEvidenceService evidenceService;

    /**
     * 创建 DGV Agent 工具 Controller。
     *
     * @param evidenceService DGV 证据领域服务
     */
    public AgentDgvToolController(AgentDgvEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    /**
     * 获取指定资产的持久化 Schema 快照。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return Schema 快照证据
     */
    @PostMapping("/api/agent/v1/tools/dataops.schema.snapshot.get:invoke")
    public AgentContract.ToolResponse<AgentDgvEvidenceService.SchemaSnapshotEvidence> schemaSnapshot(
            @RequestBody AgentContract.ToolRequest<SchemaSnapshotArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "dataops.schema.snapshot.get";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        SchemaSnapshotArguments arguments = body.arguments();
        if (arguments == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        AgentDgvEvidenceService.SchemaSnapshotEvidence evidence = evidenceService.getSchemaSnapshot(
                arguments.assetUid(), arguments.schemaVersion(), arguments.observedAt());
        return AgentContract.success(
                evidence, context, "XnetDataops/dgv", evidence.schemaVersion(), startedNanos);
    }

    /**
     * 获取指定资产的有界血缘证据。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 稳定排序的血缘证据
     */
    @PostMapping("/api/agent/v1/tools/dataops.lineage.get:invoke")
    public AgentContract.ToolResponse<AgentDgvEvidenceService.LineageEvidence> lineage(
            @RequestBody AgentContract.ToolRequest<LineageArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        String toolName = "dataops.lineage.get";
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, toolName, body);
        LineageArguments arguments = requireLineageArguments(body.arguments());
        AgentDgvEvidenceService.LineageEvidence evidence = evidenceService.getLineage(
                arguments.assetUid(), arguments.direction(), arguments.depth());
        return AgentContract.success(evidence, context, "XnetDataops/dgv", null, startedNanos);
    }

    /**
     * 校验血缘查询参数。
     *
     * @param arguments 血缘查询参数
     * @return 已校验参数
     */
    private LineageArguments requireLineageArguments(LineageArguments arguments) {
        if (arguments == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        return arguments;
    }

    /** 表示 Schema 快照查询参数。 */
    public record SchemaSnapshotArguments(String assetUid, String schemaVersion, Instant observedAt) {
    }

    /** 表示血缘查询参数。 */
    public record LineageArguments(
            String assetUid,
            AgentDgvEvidenceService.Direction direction,
            Integer depth) {
    }
}
