/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 数据版本和保留状态回归 / Dataset version and retained-state regression.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-15
 * Version: 1.0.0 | Security Level: INTERNAL
 * __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopstskservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.GovernedResourceVersionTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 验证真实版本读写一致、资源绑定和一次性迁移。 / Verify real version consistency, resource bindings and one-shot migration. */
class CompetitionDatasetToolControllerVersionTest {
    private static final String BACKFILL = "dataops.feature.backfill.start";
    private static final String VALIDATION = "dataops.dataset.validation.get";
    private static final String BUILD = "dataops.training.dataset.build";
    private static final String WORKFLOW = "dataops.workflow.instance.get";
    private static final String RISK = "asset_risk_features_prod/backfill";
    private static final String QUANT = "a-share-factor-demo-v1";
    @TempDir Path temporary;

    /** 全局懒加载时在任何业务访问前恢复历史，无关Bean不提前创建。 / Restore history before any business access under global lazy initialization without creating unrelated beans. */
    @Test void lazyApplicationRestoresBeforeAnyBusinessAccess() throws Exception {
        var fixture = legacyFixture("dataset_risk", 24);
        Path source = temporary.resolve("startup.json");
        byte[] bytes = new ObjectMapper().writeValueAsBytes(migrationDocument(fixture));
        Files.write(source, bytes);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        try (var context = lazyContext(source, digest)) {
            context.refresh();
            assertTrue(Files.exists(temporary.resolve("startup.json.consumed")));
            assertFalse(context.getBeanFactory().containsSingleton("unrelatedLazyBean"));
            var trackerField = CompetitionDatasetToolController.class.getDeclaredField("versionTracker");
            trackerField.setAccessible(true);
            var tracker = (GovernedResourceVersionTracker) trackerField.get(context.getBean(CompetitionDatasetToolController.class));
            assertEquals(fixture.tracker(), tracker.snapshot());
        }
    }

    /** 迁移错误使真实Spring懒加载刷新失败，不能延后到业务调用。 / Invalid migration fails the actual lazy Spring refresh instead of waiting for a business call. */
    @Test void lazyApplicationRejectsInvalidMigrationDuringRefresh() throws Exception {
        Path source = temporary.resolve("bad-startup.json");
        Files.writeString(source, "{}");
        try (var context = lazyContext(source, "0".repeat(64))) {
            assertThrows(org.springframework.beans.BeansException.class, context::refresh);
            assertFalse(Files.exists(temporary.resolve("bad-startup.json.consumed")));
        }
    }

    /** 使用Spring Boot真实懒加载处理器，仅替代外部服务依赖。 / Use Spring Boot's actual lazy processor while replacing only external service dependencies. */
    private org.springframework.context.annotation.AnnotationConfigApplicationContext lazyContext(Path source, String digest) {
        var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("migration-test",
                Map.of("goai.resource-state-migration-file", source.toString(), "goai.resource-state-migration-sha256", digest)));
        context.addBeanFactoryPostProcessor(new org.springframework.boot.LazyInitializationBeanFactoryPostProcessor());
        context.registerBean(GovernedApprovalVerifier.class, this::approval);
        context.registerBean(QuantitativeDatasetProductService.class, this::productService);
        context.registerBean(ObjectMapper.class, (java.util.function.Supplier<ObjectMapper>) ObjectMapper::new);
        context.registerBean("unrelatedLazyBean", StringBuilder.class);
        context.register(CompetitionDatasetToolController.class);
        return context;
    }

    /** 多次写入后新事件必须看到同一领域状态与实际版本。 / New incidents must see persistent domain state and actual versions after multiple writes. */
    @Test void newIncidentSeesActualVersionAfterMultipleWrites() {
        var controller = controller(productService());
        backfill(controller, "old", "42", "first", false, "dataset_risk");
        backfill(controller, "old", "43", "second", false, "dataset_risk");
        var response = validate(controller, "new", "dataset_risk");
        assertEquals("44", response.meta().resourceVersion());
        assertEquals(Map.of(RISK, "44"), response.data().get("resourceVersions"));
        assertEquals(true, response.data().get("valid"));
        assertEquals(24, response.data().get("timeCoverageMonths"));
        assertEquals(Map.of(RISK, "44"), controller.workflowResourceVersions("ws", "task_risk_features_latest"));
        assertThrows(AgentContractException.class, () -> backfill(controller, "new", "42", "stale", false, "dataset_risk"));
        assertThrows(AgentContractException.class, () -> validate(controller, "new", "missing"));
    }

    /** 演练不生成领域数据或改变公开版本，重放不再次执行。 / Rehearsals do not create datasets or advance public versions, and replay does not execute again. */
    @Test void rehearsalAndReplayDoNotMutateLiveState() {
        var controller = controller(productService());
        var dry = backfill(controller, "old", "42", "dry", true, "dataset_risk");
        assertEquals("43", dry.meta().resourceVersion());
        assertEquals(Map.of(RISK, "42"), dry.data().get("resourceVersions"));
        assertThrows(AgentContractException.class, () -> validate(controller, "old", "dataset_risk"));
        var first = backfill(controller, "old", "42", "first", false, "dataset_risk");
        var replay = backfill(controller, "old", "42", "first", false, "dataset_risk");
        assertEquals(first.data(), replay.data());
        assertEquals(first.auditReceipt().beforeResourceVersion(), replay.auditReceipt().beforeResourceVersion());
        assertEquals("43", validate(controller, "new", "dataset_risk").meta().resourceVersion());
    }

    /** 量化内容摘要独立于数字 CAS 版本。 / Quantitative content digests remain separate from numeric CAS versions. */
    @Test void quantitativeDigestDoesNotReplaceResourceVersion() {
        var service = productService();
        when(service.supports(QUANT)).thenReturn(true);
        when(service.buildEvidence(any(), any(), any(), any())).thenReturn(Map.of("datasetUid", QUANT, "artifactDigestSha256", "content-digest"));
        when(service.validationEvidence(QUANT)).thenReturn(Map.of("datasetUid", QUANT, "artifactDigestSha256", "content-digest", "passed", true));
        var controller = controller(service);
        var args = new CompetitionDatasetToolController.TrainingDatasetArguments("task_quant_a_share_eod_ready", QUANT, 3, List.of("new"), List.of("old"));
        var body = body(BUILD, QUANT, "42", "build", false, args);
        controller.buildTrainingDataset(body, servlet("old", body));
        var result = validate(controller, "new", QUANT);
        assertEquals("43", result.meta().resourceVersion());
        assertEquals("content-digest", result.data().get("artifactDigestSha256"));
        assertEquals(Map.of(QUANT, "43"), result.data().get("resourceVersions"));
        assertEquals(Map.of(RISK, "42"), controller.workflowResourceVersions("ws", "task_risk_features_latest"));
    }

    /** 参数目标与审批资源不符时必须在审批调用前拒绝。 / A parameter and approval target mismatch is rejected before approval verification. */
    @Test void rejectsMismatchedAndUnknownTargets() {
        var verifier = approval();
        var controller = new CompetitionDatasetToolController(verifier, productService());
        var wrong = body(BACKFILL, "different/backfill", "42", "wrong", false, backfillArguments("dataset_risk"));
        assertThrows(AgentContractException.class, () -> controller.backfillFeatures(wrong, servlet("old", wrong)));
        var unknownArgs = new CompetitionDatasetToolController.TrainingDatasetArguments("unknown-workflow", "unknown-dataset", 3, List.of(), List.of());
        var unknown = body(BUILD, "unknown-dataset", "42", "unknown", false, unknownArgs);
        assertThrows(AgentContractException.class, () -> controller.buildTrainingDataset(unknown, servlet("old", unknown)));
        var wrongTypeArgs = new CompetitionDatasetToolController.TrainingDatasetArguments("task_risk_features_latest", RISK, 3, List.of(), List.of());
        var wrongType = body(BUILD, RISK, "42", "wrong-type", false, wrongTypeArgs);
        assertThrows(AgentContractException.class, () -> controller.buildTrainingDataset(wrongType, servlet("old", wrongType)));
        verifyNoInteractions(verifier);
    }

    /** 原工作流业务证据完整保留且仅附加关联版本。 / Preserve all workflow business evidence and attach only related versions. */
    @Test void workflowEvidencePreservesBusinessFields() {
        var datasets = controller(productService());
        backfill(datasets, "old", "42", "first", false, "dataset_risk");
        var evidenceService = mock(WorkflowInstanceEvidenceService.class);
        var original = new WorkflowInstanceEvidenceService.WorkflowInstanceEvidence("task_risk_features_latest", "workflow", "name", "SUCCEEDED", "schedule",
                Instant.parse("2026-08-11T00:00:00Z"), Instant.parse("2026-08-11T00:01:00Z"), 60_000L, List.of(), List.of(), List.of("COMPETITION_SANDBOX_SNAPSHOT"));
        when(evidenceService.get("task_risk_features_latest", false)).thenReturn(original);
        var controller = new AgentWorkflowToolController(evidenceService, datasets);
        var request = body(WORKFLOW, null, null, "read", false, new AgentWorkflowToolController.WorkflowInstanceArguments("task_risk_features_latest", false));
        var result = controller.invoke(request, servlet("new", request));
        assertEquals(original.completedAt(), result.data().completedAt());
        assertEquals(original.warnings(), result.data().warnings());
        assertEquals(original.outputAssets(), result.data().outputAssets());
        assertEquals(Map.of(RISK, "43"), result.data().resourceVersions());
        assertNull(result.meta().resourceVersion());
        assertTrue(datasets.workflowResourceVersions("ws", "task_rec_features_latest").isEmpty());
    }

    /** 迁移保留领域状态和原幂等结果，不接受二次覆盖。 / Migration preserves domain state and original replay, rejecting a second restore. */
    @Test void restoresDatasetBindingAndExactReplay() {
        var fixture = legacyFixture("dataset_risk", 24);
        var controller = controller(productService());
        controller.restoreGovernedState(fixture.tracker(), fixture.domain(), fixture.bindings());
        var response = validate(controller, "new", "dataset_risk");
        assertEquals("43", response.meta().resourceVersion());
        assertEquals(true, response.data().get("passed"));
        var replay = backfill(controller, "old", "42", "first", false, "dataset_risk");
        assertEquals("43", replay.meta().resourceVersion());
        assertEquals("original", replay.data().get("retainedProof"));
        assertThrows(IllegalStateException.class, () -> controller.restoreGovernedState(fixture.tracker(), fixture.domain(), fixture.bindings()));
    }

    /** 不完整或错误绑定不能导致部分状态恢复。 / Missing or incorrect bindings cannot result in partial restoration. */
    @Test void rejectsIncompleteOrWrongMigrationWithoutPartialState() {
        var fixture = legacyFixture("dataset_risk", 24);
        var controller = controller(productService());
        assertThrows(IllegalArgumentException.class, () -> controller.restoreGovernedState(fixture.tracker(), fixture.domain(), Map.of()));
        var wrong = Map.of("ws:old:dataset_risk", new CompetitionDatasetToolController.DatasetBinding("other-workspace", "old", "dataset_risk", RISK));
        assertThrows(IllegalArgumentException.class, () -> controller.restoreGovernedState(fixture.tracker(), fixture.domain(), wrong));
        var unproven = new GovernedResourceVersionTracker.StateSnapshot(1, fixture.tracker().liveVersions(), Map.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> controller.restoreGovernedState(unproven, fixture.domain(), fixture.bindings()));
        controller.restoreGovernedState(fixture.tracker(), fixture.domain(), fixture.bindings());
        assertEquals("43", validate(controller, "new", "dataset_risk").meta().resourceVersion());
    }

    /** 不同旧事件对同一数据集存在冲突时不得任意覆盖。 / Conflicting legacy incidents for one dataset cannot be merged by iteration order. */
    @Test void rejectsConflictingLegacyDatasetStates() {
        var first = legacyFixture("dataset_risk", 24);
        var executions = new LinkedHashMap<>(first.tracker().executions());
        executions.put("ws:second:first", executions.get("ws:old:first"));
        var state = new GovernedResourceVersionTracker.StateSnapshot(1, first.tracker().liveVersions(), Map.of(), executions);
        var domains = new LinkedHashMap<>(first.domain());
        domains.put("ws:second:dataset_risk", new CompetitionDatasetToolController.LegacyDatasetState("dataset_risk", "FEATURE_BACKFILL", 36, List.of(), List.of(), true));
        var bindings = new LinkedHashMap<>(first.bindings());
        bindings.put("ws:second:dataset_risk", new CompetitionDatasetToolController.DatasetBinding("ws", "second", "dataset_risk", RISK));
        var controller = controller(productService());
        assertThrows(IllegalArgumentException.class, () -> controller.restoreGovernedState(state, domains, bindings));
        controller.restoreGovernedState(first.tracker(), first.domain(), first.bindings());
        assertEquals(24, validate(controller, "new", "dataset_risk").data().get("timeCoverageMonths"));
    }

    /** 文件摘要和一次消费标记必须在服务启动前生效。 / File hashes and one-shot consumption markers are enforced before service startup. */
    @Test void migrationFileRejectsDigestMismatchAndSecondStartup() throws Exception {
        var fixture = legacyFixture("dataset_risk", 24);
        var mapper = new ObjectMapper();
        Map<String, Object> export = migrationDocument(fixture);
        Path source = temporary.resolve("dataops-state.json");
        byte[] bytes = mapper.writeValueAsBytes(export);
        Files.write(source, bytes);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThrows(IllegalStateException.class, () -> new CompetitionDatasetToolController(approval(), productService(), mapper, source.toString(), "0".repeat(64)));
        assertFalse(Files.exists(source.resolveSibling(source.getFileName() + ".consumed")));
        var restored = new CompetitionDatasetToolController(approval(), productService(), mapper, source.toString(), digest);
        assertEquals("43", validate(restored, "new", "dataset_risk").meta().resourceVersion());
        assertTrue(Files.exists(source.resolveSibling(source.getFileName() + ".consumed")));
        assertThrows(IllegalStateException.class, () -> new CompetitionDatasetToolController(approval(), productService(), mapper, source.toString(), digest));
    }

    /** 错误来源平台即使摘要一致也不能迁移。 / A wrong source platform is rejected even when the file digest matches. */
    @Test void migrationFileRejectsWrongSourcePlatform() throws Exception {
        var mapper = new ObjectMapper();
        var export = migrationDocument(legacyFixture("dataset_risk", 24));
        export.put("platform", "aiops");
        Path source = temporary.resolve("wrong-platform.json");
        byte[] bytes = mapper.writeValueAsBytes(export);
        Files.write(source, bytes);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThrows(IllegalStateException.class, () -> new CompetitionDatasetToolController(approval(), productService(), mapper, source.toString(), digest));
        assertFalse(Files.exists(source.resolveSibling(source.getFileName() + ".consumed")));
    }

    /** 迁移字段缺失不能被反序列化为默认值。 / Missing migration fields cannot be silently replaced by deserializer defaults. */
    @Test void migrationFileRejectsMissingDomainField() throws Exception {
        var mapper = new ObjectMapper();
        var export = mapper.valueToTree(migrationDocument(legacyFixture("dataset_risk", 24)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) export.path("domainState").path("ws:old:dataset_risk")).remove("valid");
        Path source = temporary.resolve("missing-field.json");
        byte[] bytes = mapper.writeValueAsBytes(export);
        Files.write(source, bytes);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertThrows(IllegalStateException.class, () -> new CompetitionDatasetToolController(approval(), productService(), mapper, source.toString(), digest));
        assertFalse(Files.exists(source.resolveSibling(source.getFileName() + ".consumed")));
    }

    /** 创建对应旧导出格式的迁移文档。 / Create a migration document matching the old export format. */
    private Map<String, Object> migrationDocument(LegacyFixture fixture) {
        var result = new LinkedHashMap<String, Object>();
        result.put("schema", "openxnet.governed-state-export.v1");
        result.put("platform", "dataops");
        result.put("controller", CompetitionDatasetToolController.class.getName());
        result.put("sourceJarSha256", "e95e2053edc979922c599a945ead9ea9fee35816381488647cbda33ddb6d577d");
        result.put("exportedAt", "2026-09-14T16:00:00Z");
        result.put("readOnly", true);
        result.put("domainField", "datasets");
        result.put("tracker", fixture.tracker());
        result.put("domainState", fixture.domain());
        result.put("domainBindings", fixture.bindings());
        return result;
    }

    /** 创建带可核验原执行的旧状态样本。 / Create legacy state with a verifiable retained execution. */
    private LegacyFixture legacyFixture(String dataset, int months) {
        var tracker = new GovernedResourceVersionTracker();
        tracker.initializeResource("ws", RISK, 42);
        var request = body(BACKFILL, RISK, "42", "first", false, backfillArguments(dataset));
        tracker.execute(context("old", request), request, () -> Map.of("outputDatasetUid", dataset, "retainedProof", "original"));
        return new LegacyFixture(tracker.snapshot(),
                Map.of("ws:old:" + dataset, new CompetitionDatasetToolController.LegacyDatasetState(dataset, "FEATURE_BACKFILL", months, List.of(), List.of(), true)),
                Map.of("ws:old:" + dataset, new CompetitionDatasetToolController.DatasetBinding("ws", "old", dataset, RISK)));
    }

    /** 创建工具控制器。 / Create the tool controller. */
    private CompetitionDatasetToolController controller(QuantitativeDatasetProductService product) {
        return new CompetitionDatasetToolController(approval(), product);
    }

    /** 创建不连接数据库的产品服务替身。 / Create a product service test double without database connections. */
    private QuantitativeDatasetProductService productService() { return mock(QuantitativeDatasetProductService.class); }

    /** 创建通过范围内审批的替身。 / Create an approval double for valid scoped calls. */
    private GovernedApprovalVerifier approval() {
        var verifier = mock(GovernedApprovalVerifier.class);
        when(verifier.verify(any(), any())).thenReturn(new GovernedApprovalVerifier.ApprovalDecision("approval", "approver", "executor", "arguments"));
        return verifier;
    }

    /** 执行一条测试回填。 / Execute one test backfill. */
    private AgentContract.ToolResponse<Map<String, Object>> backfill(CompetitionDatasetToolController controller, String incident, String version, String key, boolean dry, String dataset) {
        var request = body(BACKFILL, RISK, version, key, dry, backfillArguments(dataset));
        return controller.backfillFeatures(request, servlet(incident, request));
    }

    /** 查询测试数据集验证。 / Query validation for a test dataset. */
    private AgentContract.ToolResponse<Map<String, Object>> validate(CompetitionDatasetToolController controller, String incident, String dataset) {
        var request = body(VALIDATION, null, null, "validate", false, new CompetitionDatasetToolController.DatasetValidationArguments(dataset));
        return controller.validateDataset(request, servlet(incident, request));
    }

    /** 创建固定资源回填参数。 / Create backfill arguments for the fixed resource. */
    private CompetitionDatasetToolController.FeatureBackfillArguments backfillArguments(String dataset) {
        return new CompetitionDatasetToolController.FeatureBackfillArguments("asset_risk_features_prod", "task_risk_features_latest", 24, "schema_121", dataset);
    }

    /** 创建带已验证上下文的请求。 / Create a servlet request with verified context. */
    private MockHttpServletRequest servlet(String incident, AgentContract.ToolRequest<?> body) {
        var request = new MockHttpServletRequest();
        request.setAttribute(AgentContract.CONTEXT_ATTRIBUTE, context(incident, body));
        return request;
    }

    /** 创建治理上下文。 / Create the governed request context. */
    private AgentContract.RequestContext context(String incident, AgentContract.ToolRequest<?> body) {
        return new AgentContract.RequestContext("ws", incident, "trace", body.toolName(), body.idempotencyKey(), "executor", "request");
    }

    /** 创建强类型工具请求。 / Create a typed tool request. */
    private <T> AgentContract.ToolRequest<T> body(String tool, String resource, String version, String key, boolean dry, T arguments) {
        return new AgentContract.ToolRequest<>("request", tool, arguments, "approval", "plan", "digest", key, resource, 1L, version, "arguments", false, "test", key, dry);
    }

    /** 包含待迁移治理与领域状态。 / Hold governed and domain state for migration. */
    private record LegacyFixture(GovernedResourceVersionTracker.StateSnapshot tracker,
            Map<String, CompetitionDatasetToolController.LegacyDatasetState> domain,
            Map<String, CompetitionDatasetToolController.DatasetBinding> bindings) { }
}
