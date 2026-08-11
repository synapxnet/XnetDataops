package com.synapxnet.dataopstskservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供量化训练数据构建、风控特征回填和数据集质量门的状态化 Live 工具。
 */
@RestController
public class CompetitionDatasetToolController {

    private final GovernedApprovalVerifier approvalVerifier;
    private final Map<String, DatasetState> datasets = new ConcurrentHashMap<>();
    private final GovernedResourceVersionTracker versionTracker = new GovernedResourceVersionTracker();

    /**
     * 创建比赛数据工具 Controller。
     *
     * @param approvalVerifier 通用计划级审批验证器
     */
    public CompetitionDatasetToolController(GovernedApprovalVerifier approvalVerifier) {
        this.approvalVerifier = approvalVerifier;
    }

    /**
     * 从固定历史窗口和因子变更构建版本化训练数据集。
     */
    @PostMapping("/api/agent/v1/tools/dataops.training.dataset.build:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> buildTrainingDataset(
            @RequestBody AgentContract.ToolRequest<TrainingDatasetArguments> body,
            HttpServletRequest servletRequest) {
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.training.dataset.build", body);
        TrainingDatasetArguments arguments = requireTrainingArguments(body.arguments());
        return executeWrite(body, context, () -> {
            DatasetState state = new DatasetState(
                    arguments.datasetUid(), "TRAINING", arguments.historyYears() * 12,
                    arguments.addedFactors(), arguments.removedFactors(), true);
            datasets.put(datasetKey(context, arguments.datasetUid()), state);
            return Map.of(
                    "datasetUid", arguments.datasetUid(),
                    "workflowInstanceUid", arguments.workflowInstanceUid(),
                    "historyYears", arguments.historyYears(),
                    "addedFactors", arguments.addedFactors(),
                    "removedFactors", arguments.removedFactors(),
                    "status", "SUCCEEDED");
        });
    }

    /**
     * 按修正版 Schema 回填固定历史窗口的特征数据。
     */
    @PostMapping("/api/agent/v1/tools/dataops.feature.backfill.start:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> backfillFeatures(
            @RequestBody AgentContract.ToolRequest<FeatureBackfillArguments> body,
            HttpServletRequest servletRequest) {
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.feature.backfill.start", body);
        FeatureBackfillArguments arguments = requireBackfillArguments(body.arguments());
        return executeWrite(body, context, () -> {
            DatasetState state = new DatasetState(
                    arguments.outputDatasetUid(), "FEATURE_BACKFILL", arguments.historyMonths(),
                    List.of("device_fingerprint_v2", "night_consumption_ratio_v2"), List.of(), true);
            datasets.put(datasetKey(context, arguments.outputDatasetUid()), state);
            return Map.of(
                    "assetUid", arguments.assetUid(),
                    "workflowInstanceUid", arguments.workflowInstanceUid(),
                    "outputDatasetUid", arguments.outputDatasetUid(),
                    "targetSchemaVersion", arguments.targetSchemaVersion(),
                    "historyMonths", arguments.historyMonths(),
                    "status", "SUCCEEDED");
        });
    }

    /**
     * 读取数据集的 Schema、质量、时间覆盖和可复现性验证结果。
     */
    @PostMapping("/api/agent/v1/tools/dataops.dataset.validation.get:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> validateDataset(
            @RequestBody AgentContract.ToolRequest<DatasetValidationArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.dataset.validation.get", body);
        DatasetValidationArguments arguments = requireValidationArguments(body.arguments());
        DatasetState state = datasets.get(datasetKey(context, arguments.datasetUid()));
        boolean valid = state != null && state.valid();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("datasetUid", arguments.datasetUid());
        data.put("status", valid ? "VALID" : "NOT_READY");
        data.put("passed", valid);
        data.put("valid", valid);
        data.put("schemaCompatible", valid);
        data.put("qualityScore", valid ? 0.998 : 0.0);
        data.put("timeCoverageMonths", state == null ? 0 : state.historyMonths());
        data.put("reproducible", valid);
        data.put("datasetType", state == null ? "UNKNOWN" : state.datasetType());
        return AgentContract.success(
                Map.copyOf(data), context, "XnetDataOps/dataset-validation", valid ? "43" : "42", startedNanos);
    }

    /**
     * 执行单个数据计划写步骤并返回平台审计回执。
     */
    private AgentContract.ToolResponse<Map<String, Object>> executeWrite(
            AgentContract.ToolRequest<?> body,
            AgentContract.RequestContext context,
            Mutation mutation) {
        long startedNanos = System.nanoTime();
        GovernedApprovalVerifier.ApprovalDecision decision = approvalVerifier.verify(body, context);
        GovernedResourceVersionTracker.Execution execution = versionTracker.execute(
                context, body, mutation::apply);
        Map<String, Object> domain = execution.data();
        long beforeVersion = execution.beforeVersion();
        long afterVersion = execution.afterVersion();
        String idempotencyKey = context.workspaceId() + ":" + context.incidentId()
                + ":" + context.idempotencyKey();
        String actionId = "dataops-" + UUID.nameUUIDFromBytes(
                idempotencyKey.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> data = new LinkedHashMap<>(domain);
        data.put("actionId", actionId);
        data.put("status", Boolean.TRUE.equals(body.dryRun()) ? "DRY_RUN" : "SUCCEEDED");
        data.put("stepId", body.stepId());
        data.put("planDigest", body.planDigest());
        AgentContract.AuditReceipt receipt = new AgentContract.AuditReceipt(
                "receipt-" + actionId, context.requestId(), context.workspaceId(), context.incidentId(),
                context.traceId(), context.toolName(), context.actorId(), decision.approverId(),
                body.approvalId(), decision.argumentsDigest(), actionId, String.valueOf(data.get("status")),
                String.valueOf(beforeVersion), String.valueOf(afterVersion), Instant.now(), Instant.now(), List.of());
        return AgentContract.successWithReceipt(
                Map.copyOf(data), context, "XnetDataOps/dataset-control", String.valueOf(afterVersion),
                startedNanos, receipt);
    }

    /**
     * 从请求中读取已验证 Agent 上下文。
     */
    private AgentContract.RequestContext context(
            HttpServletRequest servletRequest,
            String toolName,
            AgentContract.ToolRequest<?> body) {
        return AgentContract.requireContext(servletRequest, toolName, body);
    }

    /**
     * 构建 Workspace 与 Incident 隔离的数据集键。
     */
    private String datasetKey(AgentContract.RequestContext context, String datasetUid) {
        return context.workspaceId() + ":" + context.incidentId() + ":" + datasetUid;
    }

    /** 校验训练数据集参数。 */
    private TrainingDatasetArguments requireTrainingArguments(TrainingDatasetArguments value) {
        if (value == null || value.historyYears() == null || value.historyYears() < 1
                || value.historyYears() > 20 || value.addedFactors() == null || value.removedFactors() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "训练数据集参数无效");
        }
        requireText(value.workflowInstanceUid(), "workflowInstanceUid");
        requireText(value.datasetUid(), "datasetUid");
        return value;
    }

    /** 校验特征回填参数。 */
    private FeatureBackfillArguments requireBackfillArguments(FeatureBackfillArguments value) {
        if (value == null || value.historyMonths() == null || value.historyMonths() < 1
                || value.historyMonths() > 120) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "特征回填参数无效");
        }
        requireText(value.assetUid(), "assetUid");
        requireText(value.workflowInstanceUid(), "workflowInstanceUid");
        requireText(value.targetSchemaVersion(), "targetSchemaVersion");
        requireText(value.outputDatasetUid(), "outputDatasetUid");
        return value;
    }

    /** 校验数据集验证参数。 */
    private DatasetValidationArguments requireValidationArguments(DatasetValidationArguments value) {
        if (value == null) throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        requireText(value.datasetUid(), "datasetUid");
        return value;
    }

    /** 校验必填有界文本。 */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 512 || value.indexOf('\0') >= 0) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", field + " 无效");
        }
    }

    /** 表示审批通过后的数据状态修改函数。 */
    @FunctionalInterface
    private interface Mutation {
        Map<String, Object> apply();
    }

    /** 保存已构建数据集的最小可审计状态。 */
    private record DatasetState(
            String datasetUid,
            String datasetType,
            int historyMonths,
            List<String> addedFeatures,
            List<String> removedFeatures,
            boolean valid) { }

    /** 训练数据集构建参数。 */
    public record TrainingDatasetArguments(String workflowInstanceUid, String datasetUid, Integer historyYears, List<String> addedFactors, List<String> removedFactors) { }
    /** 特征历史回填参数。 */
    public record FeatureBackfillArguments(String assetUid, String workflowInstanceUid, Integer historyMonths, String targetSchemaVersion, String outputDatasetUid) { }
    /** 数据集验证参数。 */
    public record DatasetValidationArguments(String datasetUid) { }
}
