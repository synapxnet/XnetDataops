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
import com.synapxnet.goai.contract.FeatureDriftRuntimeClient;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@org.springframework.context.annotation.Lazy(false)
@RestController
public class CompetitionDatasetToolController {
    // 仅启用的真实运行时分流，缺失响应不得回退。 Route only enabled real execution; never fall back on missing evidence.
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FeatureDriftRuntimeClient featureDriftRuntime;


    private volatile boolean legacyStateUnavailable;
    private final GovernedApprovalVerifier approvalVerifier;
    private final QuantitativeDatasetProductService quantitativeDatasetProductService;
    private final Map<String, DatasetState> datasets = new ConcurrentHashMap<>();
    private final GovernedResourceVersionTracker versionTracker = new GovernedResourceVersionTracker();

    private static final Map<String, String> WORKFLOW_RESOURCES = Map.of(
            "task_quant_a_share_eod_ready", "a-share-factor-demo-v1",
            "task_risk_features_latest", "asset_risk_features_prod/backfill");

    /**
     * 创建数据工具控制器。 / Create the dataset tool controller.
     *
     * @param approvalVerifier 通用计划级审批验证器
     * @param quantitativeDatasetProductService 真实 A 股研究数据产品查询服务
     */
    public CompetitionDatasetToolController(
            GovernedApprovalVerifier approvalVerifier,
            QuantitativeDatasetProductService quantitativeDatasetProductService) {
        this.approvalVerifier = approvalVerifier;
        this.quantitativeDatasetProductService = quantitativeDatasetProductService;
    }

    /** 启动时仅从显式匹配摘要的一次性迁移文件恢复。 / Restore at startup only from an explicit one-shot migration file with a matching digest. */
    public CompetitionDatasetToolController(GovernedApprovalVerifier approvalVerifier,
            QuantitativeDatasetProductService quantitativeDatasetProductService, ObjectMapper mapper,
            @Value("${goai.resource-state-migration-file:}") String migrationFile,
            @Value("${goai.resource-state-migration-sha256:}") String migrationSha256) {
        this(approvalVerifier, quantitativeDatasetProductService, mapper, migrationFile, migrationSha256, false);
    }

    /** 显式真实运行时可隔离未知旧状态，保留普通服务。 / Explicit real execution may quarantine unknown legacy state while retaining native services. */
    @Autowired
    public CompetitionDatasetToolController(GovernedApprovalVerifier approvalVerifier,
            QuantitativeDatasetProductService quantitativeDatasetProductService, ObjectMapper mapper,
            @Value("${goai.resource-state-migration-file:}") String migrationFile,
            @Value("${goai.resource-state-migration-sha256:}") String migrationSha256,
            @Value("${OPENXNET_FEATURE_DRIFT_ENABLED:false}") boolean realRuntimeEnabled) {
        this(approvalVerifier, quantitativeDatasetProductService);
        restoreMigration(mapper, migrationFile, migrationSha256, realRuntimeEnabled);
    }

    /** 未知旧治理状态不能初始化或返回成功。 / Unknown legacy governance state cannot initialize or return success. */
    private void requireLegacyState() {
        if (legacyStateUnavailable) throw new AgentContractException(503, "STATE_UNAVAILABLE",
                "旧治理状态缺少最新持久快照，已隔离；未回放旧迁移或重置资源版本。");
    }

    /** 校验完整迁移文件、来源和消费标记，失败时阻止启动。 / Verify the complete migration file, source and consumption marker, failing startup on error. */
    private void restoreMigration(ObjectMapper mapper, String migrationFile, String expectedSha256, boolean realRuntimeEnabled) {
        if ((migrationFile == null || migrationFile.isBlank()) && (expectedSha256 == null || expectedSha256.isBlank())) return;
        try {
            if (migrationFile == null || migrationFile.isBlank() || expectedSha256 == null || !expectedSha256.matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("An explicit migration file and SHA-256 are both required.");
            }
            java.nio.file.Path source = java.nio.file.Path.of(migrationFile);
            if (!source.isAbsolute() || !java.nio.file.Files.isRegularFile(source, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    || java.nio.file.Files.size(source) > 4 * 1024 * 1024) throw new IllegalArgumentException("Migration file is invalid.");
            for (java.nio.file.Path cursor = source; cursor != null; cursor = cursor.getParent()) {
                if (java.nio.file.Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("Migration path cannot contain links.");
            }
            java.nio.file.Path consumed = source.resolveSibling(source.getFileName() + ".consumed");
            byte[] bytes = java.nio.file.Files.readAllBytes(source);
            String actual = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!expectedSha256.equals(actual)) throw new IllegalArgumentException("Migration digest does not match.");
            ObjectMapper strictMapper = mapper.copy().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .disable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            JsonNode root = strictMapper.readTree(bytes);
            if (!"openxnet.governed-state-export.v1".equals(root.path("schema").asText())
                    || !"dataops".equals(root.path("platform").asText())
                    || !CompetitionDatasetToolController.class.getName().equals(root.path("controller").asText())
                    || !"datasets".equals(root.path("domainField").asText()) || !root.path("readOnly").asBoolean()
                    || !"e95e2053edc979922c599a945ead9ea9fee35816381488647cbda33ddb6d577d".equals(root.path("sourceJarSha256").asText())
                    || !root.path("domainState").isObject() || !root.path("domainBindings").isObject()) {
                throw new IllegalArgumentException("Migration schema, platform or source artifact does not match.");
            }
            Instant.parse(root.path("exportedAt").asText());
            if (java.nio.file.Files.exists(consumed, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                if (!realRuntimeEnabled || !java.nio.file.Files.isRegularFile(consumed, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                        || java.nio.file.Files.size(consumed) > 66
                        || !actual.equals(java.nio.file.Files.readString(consumed, StandardCharsets.UTF_8).trim())) {
                    throw new IllegalStateException("Consumed migration cannot provide current governed state.");
                }
                legacyStateUnavailable = true;
                return;
            }
            restoreGovernedState(strictMapper.treeToValue(root.path("tracker"), GovernedResourceVersionTracker.StateSnapshot.class),
                    strictMapper.convertValue(root.path("domainState"), new TypeReference<Map<String, LegacyDatasetState>>() { }),
                    strictMapper.convertValue(root.path("domainBindings"), new TypeReference<Map<String, DatasetBinding>>() { }));
            java.nio.file.Files.writeString(consumed, actual + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (Exception error) {
            throw new IllegalStateException("Governed DataOps state migration refused; service startup is stopped.", error);
        }
    }

    /**
     * 构建版本化训练数据集。 / Build a versioned training dataset.
     */
    @PostMapping("/api/agent/v1/tools/dataops.training.dataset.build:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> buildTrainingDataset(
            @RequestBody AgentContract.ToolRequest<TrainingDatasetArguments> body,
            HttpServletRequest servletRequest) {
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.training.dataset.build", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        requireLegacyState();
        TrainingDatasetArguments arguments = requireTrainingArguments(body.arguments());
        requireCanonicalTarget(body, arguments.workflowInstanceUid(), arguments.datasetUid());
        if (!"a-share-factor-demo-v1".equals(arguments.datasetUid())) {
            throw new AgentContractException(404, "RESOURCE_NOT_REGISTERED", "训练构建目标未登记为训练数据产品");
        }
        return executeWrite(body, context, () -> {
            if (quantitativeDatasetProductService.supports(arguments.datasetUid())) {
                return quantitativeDatasetProductService.buildEvidence(
                        arguments.datasetUid(),
                        arguments.workflowInstanceUid(),
                        arguments.addedFactors(),
                        arguments.removedFactors());
            }
            DatasetState state = new DatasetState(
                    arguments.datasetUid(), arguments.datasetUid(), "TRAINING", arguments.historyYears() * 12,
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
     * 按修正版 Schema 回填历史特征。 / Backfill historical features using the corrected schema.
     */
    @PostMapping("/api/agent/v1/tools/dataops.feature.backfill.start:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> backfillFeatures(
            @RequestBody AgentContract.ToolRequest<FeatureBackfillArguments> body,
            HttpServletRequest servletRequest) {
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.feature.backfill.start", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        requireLegacyState();
        FeatureBackfillArguments arguments = requireBackfillArguments(body.arguments());
        String canonicalResourceId = arguments.assetUid() + "/backfill";
        requireCanonicalTarget(body, arguments.workflowInstanceUid(), canonicalResourceId);
        return executeWrite(body, context, () -> {
            DatasetState previous = datasets.get(datasetKey(context, arguments.outputDatasetUid()));
            if (previous != null && !previous.canonicalResourceId().equals(canonicalResourceId)) {
                throw new AgentContractException(409, "DATASET_RESOURCE_CONFLICT", "输出数据集已绑定其他资源");
            }
            DatasetState state = new DatasetState(
                    arguments.outputDatasetUid(), canonicalResourceId, "FEATURE_BACKFILL", arguments.historyMonths(),
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
     * 一致读取数据集验证结果和真实资源版本。 / Read dataset validation and its actual resource version consistently.
     */
    @PostMapping("/api/agent/v1/tools/dataops.dataset.validation.get:invoke")
    public AgentContract.ToolResponse<Map<String, Object>> validateDataset(
            @RequestBody AgentContract.ToolRequest<DatasetValidationArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = context(
                servletRequest, "dataops.dataset.validation.get", body);
        if (featureDriftRuntime != null && featureDriftRuntime.handles(context.toolName(), body.arguments())) {
            return featureDriftRuntime.invoke(body, context);
        }
        requireLegacyState();
        DatasetValidationArguments arguments = requireValidationArguments(body.arguments());
        return versionTracker.readConsistently(() -> validationResponse(arguments.datasetUid(), context, startedNanos));
    }

    /** 在版本锁内构建数据集验证响应。 / Build a dataset validation response while holding the version lock. */
    private AgentContract.ToolResponse<Map<String, Object>> validationResponse(
            String datasetUid, AgentContract.RequestContext context, long startedNanos) {
        if (quantitativeDatasetProductService.supports(datasetUid)) {
            registerKnownResource(context.workspaceId(), datasetUid);
            Map<String, Object> data = new LinkedHashMap<>(quantitativeDatasetProductService.validationEvidence(datasetUid));
            Map<String, String> versions = resourceVersions(context.workspaceId(), datasetUid);
            data.put("resourceVersions", versions);
            return AgentContract.success(Map.copyOf(data), context, "XnetDataOps/quantitative-dataset-validation",
                    versions.get(datasetUid), startedNanos);
        }
        DatasetState state = datasets.get(datasetKey(context, datasetUid));
        if (state == null) throw new AgentContractException(404, "DATASET_NOT_REGISTERED", "数据集尚未构建或迁移");
        boolean valid = state.valid();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("datasetUid", datasetUid);
        data.put("status", valid ? "VALID" : "NOT_READY");
        data.put("passed", valid);
        data.put("valid", valid);
        data.put("schemaCompatible", valid);
        data.put("qualityScore", valid ? 0.998 : 0.0);
        data.put("timeCoverageMonths", state.historyMonths());
        data.put("reproducible", valid);
        data.put("datasetType", state.datasetType());
        Map<String, String> versions = resourceVersions(context.workspaceId(), state.canonicalResourceId());
        data.put("resourceVersions", versions);
        return AgentContract.success(
                Map.copyOf(data), context, "XnetDataOps/dataset-validation", versions.get(state.canonicalResourceId()), startedNanos);
    }

    /**
     * 验证审批后原子执行并返回审计回执。 / Verify approval before atomic execution and an audit receipt.
     */
    private AgentContract.ToolResponse<Map<String, Object>> executeWrite(
            AgentContract.ToolRequest<?> body,
            AgentContract.RequestContext context,
            Mutation mutation) {
        long startedNanos = System.nanoTime();
        GovernedApprovalVerifier.ApprovalDecision decision = approvalVerifier.verify(body, context);
        return versionTracker.readConsistently(() -> {
            registerKnownResource(context.workspaceId(), body.resourceId());
            return executeApproved(body, context, mutation, startedNanos, decision);
        });
    }

    /** 在同一版本锁内执行审批步骤并封装结果。 / Execute an approved step and form its response under the same version lock. */
    private AgentContract.ToolResponse<Map<String, Object>> executeApproved(
            AgentContract.ToolRequest<?> body, AgentContract.RequestContext context, Mutation mutation,
            long startedNanos, GovernedApprovalVerifier.ApprovalDecision decision) {
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
        data.put("resourceVersions", resourceVersions(context.workspaceId(), body.resourceId()));
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
     * 读取已验证 Agent 上下文。 / Read the verified agent context.
     */
    private AgentContract.RequestContext context(
            HttpServletRequest servletRequest,
            String toolName,
            AgentContract.ToolRequest<?> body) {
        return AgentContract.requireContext(servletRequest, toolName, body);
    }

    /**
     * 构建 Workspace 内持续的数据集键。 / Build a persistent dataset key within one workspace.
     */
    private String datasetKey(AgentContract.RequestContext context, String datasetUid) {
        return context.workspaceId() + ":" + datasetUid;
    }

    /** 读取固定工作流关联目标的真实版本。 / Read actual versions of the targets explicitly bound to a workflow. */
    public Map<String, String> workflowResourceVersions(String workspaceId, String instanceUid) {
        requireLegacyState();
        return versionTracker.readConsistently(() -> {
            String canonical = WORKFLOW_RESOURCES.get(instanceUid);
            if (canonical == null) return Map.of();
            registerKnownResource(workspaceId, canonical);
            return resourceVersions(workspaceId, canonical);
        });
    }

    /** 同一锁内读取领域证据及治理版本。 / Read domain evidence and governed versions under the same lock. */
    public <T> T readConsistently(java.util.function.Supplier<T> reader) {
        requireLegacyState();
        return versionTracker.readConsistently(reader);
    }

    /** 仅为明确登记目标建立平台初始版本。 / Initialize platform-owned versions only for explicitly registered targets. */
    private void registerKnownResource(String workspaceId, String canonicalResourceId) {
        requireLegacyState();
        if (!WORKFLOW_RESOURCES.containsValue(canonicalResourceId)) {
            throw new AgentContractException(404, "RESOURCE_NOT_REGISTERED", "资源未列入平台登记关联");
        }
        versionTracker.initializeResource(workspaceId, canonicalResourceId, 42L);
    }

    /** 确保工具参数、审批资源和固定工作流关联一致。 / Require matching arguments, approval resource and registered workflow association. */
    private void requireCanonicalTarget(AgentContract.ToolRequest<?> body, String workflowUid, String canonical) {
        if (!canonical.equals(body.resourceId())) {
            throw new AgentContractException(400, "RESOURCE_ID_MISMATCH", "审批资源与工具参数指向的目标不一致");
        }
        if (!canonical.equals(WORKFLOW_RESOURCES.get(workflowUid))) {
            throw new AgentContractException(404, "RESOURCE_NOT_REGISTERED", "工作流与目标资源未登记关联");
        }
    }

    /** 从唯一写入版本源生成公开版本映射。 / Build public version maps from the single write-side version source. */
    private Map<String, String> resourceVersions(String workspaceId, String canonical) {
        return Map.of(canonical, Long.toString(versionTracker.currentVersion(workspaceId, canonical)));
    }

    /** 校验全部旧数据绑定后原子恢复，拒绝覆盖已有状态。 / Validate all legacy bindings before atomic restore and reject existing state. */
    public void restoreGovernedState(GovernedResourceVersionTracker.StateSnapshot trackerState,
            Map<String, LegacyDatasetState> legacy, Map<String, DatasetBinding> bindings) {
        versionTracker.readConsistently(() -> {
            if (!datasets.isEmpty() || !versionTracker.snapshot().liveVersions().isEmpty()) {
                throw new IllegalStateException("Dataset restoration requires empty state.");
            }
            if (legacy == null || bindings == null || legacy.size() > 4096 || !legacy.keySet().equals(bindings.keySet())) {
                throw new IllegalArgumentException("Every legacy dataset requires one verified binding.");
            }
            GovernedResourceVersionTracker validated = new GovernedResourceVersionTracker();
            validated.restore(trackerState);
            Map<String, DatasetState> migrated = new LinkedHashMap<>();
            for (Map.Entry<String, LegacyDatasetState> entry : legacy.entrySet()) {
                LegacyDatasetState old = entry.getValue();
                DatasetBinding binding = bindings.get(entry.getKey());
                if (old == null || binding == null) throw new IllegalArgumentException("Missing dataset binding or state.");
                requireText(binding.workspaceId(), "workspaceId");
                requireText(binding.incidentId(), "incidentId");
                requireText(binding.datasetUid(), "datasetUid");
                requireText(binding.canonicalResourceId(), "canonicalResourceId");
                if (binding.workspaceId().contains(":") || binding.incidentId().contains(":")
                        || !entry.getKey().equals(binding.workspaceId() + ":" + binding.incidentId() + ":" + binding.datasetUid())
                        || !binding.datasetUid().equals(old.datasetUid()) || old.historyMonths() < 1 || old.historyMonths() > 240
                        || old.addedFeatures() == null || old.removedFeatures() == null) {
                    throw new IllegalArgumentException("Legacy dataset scope or state is invalid.");
                }
                if (!("TRAINING".equals(old.datasetType()) && old.datasetUid().equals(binding.canonicalResourceId()))
                        && !("FEATURE_BACKFILL".equals(old.datasetType()) && binding.canonicalResourceId().endsWith("/backfill"))) {
                    throw new IllegalArgumentException("Legacy dataset type and canonical resource do not match.");
                }
                validated.currentVersion(binding.workspaceId(), binding.canonicalResourceId());
                boolean associated = trackerState.executions().entrySet().stream().anyMatch(execution ->
                        execution.getKey().startsWith(binding.workspaceId() + ":" + binding.incidentId() + ":")
                        && !execution.getValue().dryRun() && execution.getValue().resourceId().equals(binding.canonicalResourceId())
                        && binding.datasetUid().equals(execution.getValue().data().get("FEATURE_BACKFILL".equals(old.datasetType()) ? "outputDatasetUid" : "datasetUid")));
                if (!associated) throw new IllegalArgumentException("Legacy dataset lacks a matching retained live execution.");
                DatasetState state = new DatasetState(old.datasetUid(), binding.canonicalResourceId(), old.datasetType(), old.historyMonths(),
                        List.copyOf(old.addedFeatures()), List.copyOf(old.removedFeatures()), old.valid());
                DatasetState duplicate = migrated.putIfAbsent(binding.workspaceId() + ":" + old.datasetUid(), state);
                if (duplicate != null && !duplicate.equals(state)) throw new IllegalArgumentException("Conflicting legacy dataset states cannot be merged.");
            }
            versionTracker.restore(validated.snapshot());
            datasets.putAll(migrated);
            return null;
        });
    }

    /** 校验训练数据集参数。 / Validate training dataset arguments. */
    private TrainingDatasetArguments requireTrainingArguments(TrainingDatasetArguments value) {
        if (value == null || value.historyYears() == null || value.historyYears() < 1
                || value.historyYears() > 20 || value.addedFactors() == null || value.removedFactors() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "训练数据集参数无效");
        }
        requireText(value.workflowInstanceUid(), "workflowInstanceUid");
        requireText(value.datasetUid(), "datasetUid");
        return value;
    }

    /** 校验特征回填参数。 / Validate feature backfill arguments. */
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

    /** 校验数据集验证参数。 / Validate dataset validation arguments. */
    private DatasetValidationArguments requireValidationArguments(DatasetValidationArguments value) {
        if (value == null) throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        requireText(value.datasetUid(), "datasetUid");
        return value;
    }

    /** 校验必填有界文本。 / Validate required bounded text. */
    private void requireText(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 512 || value.indexOf('\0') >= 0) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", field + " 无效");
        }
    }

    /** 表示审批后的状态修改函数。 / Represent an approved state mutation. */
    @FunctionalInterface
    private interface Mutation {
        /** 执行领域修改。 / Apply the domain mutation. */
        Map<String, Object> apply();
    }

    /** 保存数据集与规范目标关联的状态。 / Retain dataset state bound to its canonical target. */
    private record DatasetState(
            String datasetUid,
            String canonicalResourceId,
            String datasetType,
            int historyMonths,
            List<String> addedFeatures,
            List<String> removedFeatures,
            boolean valid) { }

    /** 兼容旧进程六字段数据集导出。 / Match the six-field dataset export from the old process. */
    public record LegacyDatasetState(String datasetUid, String datasetType, int historyMonths,
            List<String> addedFeatures, List<String> removedFeatures, boolean valid) { }

    /** 由已验证证据提供旧记录的完整资源关联。 / Supply complete legacy resource associations from verified evidence. */
    public record DatasetBinding(String workspaceId, String incidentId, String datasetUid, String canonicalResourceId) { }

    /** 训练数据集构建参数。
     * English: Training Dataset Arguments.
     */
    public record TrainingDatasetArguments(String workflowInstanceUid, String datasetUid, Integer historyYears, List<String> addedFactors, List<String> removedFactors) { }
    /** 特征历史回填参数。
     * English: Feature Backfill Arguments.
     */
    public record FeatureBackfillArguments(String assetUid, String workflowInstanceUid, Integer historyMonths, String targetSchemaVersion, String outputDatasetUid) { }
    /** 数据集验证参数。
     * English: Dataset Validation Arguments.
     */
    public record DatasetValidationArguments(String datasetUid) { }
}
