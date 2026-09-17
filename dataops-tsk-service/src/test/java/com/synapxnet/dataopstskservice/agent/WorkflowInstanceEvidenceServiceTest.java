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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopstskservice.entity.NodeInstance;
import com.synapxnet.dataopstskservice.entity.TaskInstance;
import com.synapxnet.dataopstskservice.entity.Workflow;
import com.synapxnet.dataopstskservice.entity.WorkflowNode;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证批量节点读取、日志脱敏和持久化产出资产解析。
 */
class WorkflowInstanceEvidenceServiceTest {

    /** 日志必须先脱敏，产出资产必须从节点 JSON 而不是 UID 分支获得。
     * English: Redacts Logs And Returns Output Asset.
     */
    @Test
    void redactsLogsAndReturnsOutputAsset() {
        WorkflowService workflowService = mock(WorkflowService.class);
        Fixture fixture = fixture(workflowService, true);
        WorkflowInstanceEvidenceService service = new WorkflowInstanceEvidenceService(
                workflowService, new ObjectMapper());

        WorkflowInstanceEvidenceService.WorkflowInstanceEvidence result = service.get(
                "task_risk_features_latest", true);

        String log = result.nodes().get(0).logSummary();
        assertFalse(log.contains("secret-value"));
        assertFalse(log.contains("13800138000"));
        assertFalse(log.contains("risk@example.com"));
        assertEquals("asset_risk_features_prod", result.outputAssets().get(0).assetUid());
        assertEquals("schema_risk_features_120", result.outputAssets().get(0).schemaSnapshotUid());
        verify(workflowService).getNodeInstances(fixture.instance().getId(), true);
    }

    /** 关闭日志时 Service 必须调用不读取 log_content 的 Mapper 路径。
     * English: Avoids Log Content When Not Requested.
     */
    @Test
    void avoidsLogContentWhenNotRequested() {
        WorkflowService workflowService = mock(WorkflowService.class);
        Fixture fixture = fixture(workflowService, false);
        WorkflowInstanceEvidenceService service = new WorkflowInstanceEvidenceService(
                workflowService, new ObjectMapper());

        WorkflowInstanceEvidenceService.WorkflowInstanceEvidence result = service.get(
                "task_risk_features_latest", false);

        assertNull(result.nodes().get(0).logSummary());
        verify(workflowService).getNodeInstances(fixture.instance().getId(), false);
    }

    /** 固定比赛 UID 缺少数据库记录时必须返回带来源标记的隔离沙盘证据。
     * English: Returns Whitelisted Competition Sandbox Evidence.
     */
    @Test
    void returnsWhitelistedCompetitionSandboxEvidence() {
        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.getInstanceByUid("task_rec_features_latest"))
                .thenThrow(new IllegalArgumentException("missing"));
        WorkflowInstanceEvidenceService service = new WorkflowInstanceEvidenceService(
                workflowService, new ObjectMapper());

        WorkflowInstanceEvidenceService.WorkflowInstanceEvidence result = service.get(
                "task_rec_features_latest", true);

        assertEquals("SUCCEEDED", result.status());
        assertEquals("asset_rec_features_prod", result.outputAssets().get(0).assetUid());
        assertEquals("COMPETITION_SANDBOX_SNAPSHOT", result.warnings().get(0));
        assertEquals("比赛隔离沙盘任务已完成，产出版本化数据资产。",
                result.nodes().get(0).logSummary());
        verify(workflowService).getInstanceByUid("task_rec_features_latest");
        verifyNoMoreInteractions(workflowService);
    }

    /** 非比赛白名单 UID 缺少数据库记录时必须继续返回不存在错误。
     * English: Rejects Unknown Missing Instance.
     */
    @Test
    void rejectsUnknownMissingInstance() {
        WorkflowService workflowService = mock(WorkflowService.class);
        when(workflowService.getInstanceByUid("task_unknown"))
                .thenThrow(new IllegalArgumentException("missing"));
        WorkflowInstanceEvidenceService service = new WorkflowInstanceEvidenceService(
                workflowService, new ObjectMapper());

        assertThrows(com.synapxnet.goai.contract.AgentContractException.class,
                () -> service.get("task_unknown", false));
        verify(workflowService).getInstanceByUid("task_unknown");
        verifyNoMoreInteractions(workflowService);
    }

    /** 创建稳定工作流、实例、节点和产出配置。
     * English: Fixture.
     */
    private Fixture fixture(WorkflowService service, boolean includeLog) {
        Workflow workflow = new Workflow();
        workflow.setId(3L);
        workflow.setUid("workflow_risk_features");
        workflow.setName("风险特征生产链路");
        TaskInstance instance = new TaskInstance();
        instance.setId(5L);
        instance.setUid("task_risk_features_latest");
        instance.setWorkflowId(workflow.getId());
        instance.setStatus("success");
        instance.setTriggerType("schedule");
        instance.setStartTime(LocalDateTime.of(2026, 8, 2, 9, 58));
        instance.setEndTime(LocalDateTime.of(2026, 8, 2, 10, 0));
        NodeInstance node = new NodeInstance();
        node.setNodeKey("publish");
        node.setStatus("success");
        node.setStartTime(instance.getStartTime());
        node.setEndTime(instance.getEndTime());
        if (includeLog) {
            node.setLogContent("password=secret-value token=secret-value 13800138000 risk@example.com");
        }
        WorkflowNode definition = new WorkflowNode();
        definition.setNodeKey("publish");
        definition.setConfigJson("""
                {"outputAssetUid":"asset_risk_features_prod",\
                 "schemaSnapshotUid":"schema_risk_features_120"}
                """);
        when(service.getInstanceByUid(instance.getUid())).thenReturn(instance);
        when(service.getById(workflow.getId())).thenReturn(workflow);
        when(service.getNodeInstances(instance.getId(), includeLog)).thenReturn(List.of(node));
        when(service.getNodes(workflow.getId())).thenReturn(List.of(definition));
        return new Fixture(instance);
    }

    /** 保存测试中需要复用的 Entity。
     * English: Fixture.
     */
    private record Fixture(TaskInstance instance) {
    }
}
