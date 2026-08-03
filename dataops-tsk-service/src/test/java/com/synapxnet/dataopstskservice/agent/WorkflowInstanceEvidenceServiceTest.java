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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证批量节点读取、日志脱敏和持久化产出资产解析。
 */
class WorkflowInstanceEvidenceServiceTest {

    /** 日志必须先脱敏，产出资产必须从节点 JSON 而不是 UID 分支获得。 */
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

    /** 关闭日志时 Service 必须调用不读取 log_content 的 Mapper 路径。 */
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

    /** 创建稳定工作流、实例、节点和产出配置。 */
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

    /** 保存测试中需要复用的 Entity。 */
    private record Fixture(TaskInstance instance) {
    }
}
