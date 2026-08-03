package com.synapxnet.dataopstskservice.service;

import com.synapxnet.dataopstskservice.entity.*;
import java.util.List;

public interface WorkflowService {
    List<Workflow> listAll();
    Workflow getById(Long id);
    Workflow create(Workflow workflow);
    Workflow update(Workflow workflow);
    void delete(Long id);
    void updateStatus(Long id, String status);
    List<WorkflowNode> getNodes(Long workflowId);
    List<WorkflowEdge> getEdges(Long workflowId);
    void saveDAG(Long workflowId, List<WorkflowNode> nodes, List<WorkflowEdge> edges);
    List<TaskInstance> listInstances(Long workflowId);
    List<TaskInstance> listRecentInstances();
    TaskInstance getInstance(Long id);
    /**
     * 根据稳定 UID 获取任务实例。
     *
     * @param uid 任务实例 UID
     * @return 任务实例
     */
    TaskInstance getInstanceByUid(String uid);
    TaskInstance triggerWorkflow(Long workflowId, String triggerType);
    List<NodeInstance> getNodeInstances(Long taskInstanceId);
    /**
     * 按需读取节点实例日志；关闭时 SQL 完全不选择大日志字段。
     *
     * @param taskInstanceId 任务实例数字 ID
     * @param includeLogSummary 是否读取日志字段
     * @return 节点实例列表
     */
    List<NodeInstance> getNodeInstances(Long taskInstanceId, boolean includeLogSummary);
}
