package com.synapxnet.dataopstskservice.service.impl;

import com.synapxnet.dataopstskservice.entity.*;
import com.synapxnet.dataopstskservice.mapper.WorkflowMapper;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowServiceImpl implements WorkflowService {
    private final WorkflowMapper workflowMapper;

    public WorkflowServiceImpl(WorkflowMapper workflowMapper) { this.workflowMapper = workflowMapper; }

    @Override public List<Workflow> listAll() { return workflowMapper.findAll(); }

    @Override public Workflow getById(Long id) {
        Workflow w = workflowMapper.findById(id);
        if (w == null) throw new IllegalArgumentException("Workflow not found: " + id);
        return w;
    }

    @Override public Workflow create(Workflow workflow) {
        workflow.setUid(UUID.randomUUID().toString());
        if (workflow.getStatus() == null) workflow.setStatus("draft");
        workflowMapper.insert(workflow);
        return workflow;
    }

    @Override public Workflow update(Workflow workflow) {
        workflowMapper.update(workflow);
        return workflowMapper.findById(workflow.getId());
    }

    @Override public void delete(Long id) { workflowMapper.deleteById(id); }

    @Override public void updateStatus(Long id, String status) { workflowMapper.updateStatus(id, status); }

    @Override public List<WorkflowNode> getNodes(Long workflowId) { return workflowMapper.findNodesByWorkflowId(workflowId); }
    @Override public List<WorkflowEdge> getEdges(Long workflowId) { return workflowMapper.findEdgesByWorkflowId(workflowId); }

    @Override public void saveDAG(Long workflowId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        workflowMapper.deleteNodesByWorkflowId(workflowId);
        workflowMapper.deleteEdgesByWorkflowId(workflowId);
        for (WorkflowNode n : nodes) { n.setWorkflowId(workflowId); workflowMapper.insertNode(n); }
        for (WorkflowEdge e : edges) { e.setWorkflowId(workflowId); workflowMapper.insertEdge(e); }
    }

    @Override public List<TaskInstance> listInstances(Long workflowId) {
        if (workflowId != null) return workflowMapper.findInstancesByWorkflowId(workflowId);
        return workflowMapper.findRecentInstances();
    }

    @Override public List<TaskInstance> listRecentInstances() { return workflowMapper.findRecentInstances(); }

    @Override public TaskInstance getInstance(Long id) {
        TaskInstance ti = workflowMapper.findInstanceById(id);
        if (ti == null) throw new IllegalArgumentException("TaskInstance not found: " + id);
        return ti;
    }

    @Override public TaskInstance triggerWorkflow(Long workflowId, String triggerType) {
        getById(workflowId);
        TaskInstance instance = new TaskInstance();
        instance.setUid(UUID.randomUUID().toString());
        instance.setWorkflowId(workflowId);
        instance.setStatus("pending");
        instance.setTriggerType(triggerType != null ? triggerType : "manual");
        workflowMapper.insertInstance(instance);
        return instance;
    }

    @Override public List<NodeInstance> getNodeInstances(Long taskInstanceId) {
        return workflowMapper.findNodeInstancesByTaskId(taskInstanceId);
    }
}
