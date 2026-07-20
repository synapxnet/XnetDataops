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
    TaskInstance triggerWorkflow(Long workflowId, String triggerType);
    List<NodeInstance> getNodeInstances(Long taskInstanceId);
}
