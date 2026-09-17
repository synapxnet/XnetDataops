package com.synapxnet.dataopstskservice.service.impl;

import com.synapxnet.dataopstskservice.entity.*;
import com.synapxnet.dataopstskservice.mapper.WorkflowMapper;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
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

    /** 验证完整图后在单一事务内替换，失败回滚旧图。 Validate the complete graph and replace it transactionally, rolling back on failure. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDAG(Long workflowId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        getById(workflowId);
        validateDAG(nodes, edges);
        workflowMapper.deleteEdgesByWorkflowId(workflowId);
        workflowMapper.deleteNodesByWorkflowId(workflowId);
        for (WorkflowNode node : nodes) {
            node.setId(null);
            node.setWorkflowId(workflowId);
            workflowMapper.insertNode(node);
        }
        for (WorkflowEdge edge : edges) {
            edge.setId(null);
            edge.setWorkflowId(workflowId);
            workflowMapper.insertEdge(edge);
        }
    }

    /** 拒绝非法节点、配置、悬空依赖、重复边与循环。 Reject invalid nodes, configuration, dangling references, duplicates and cycles. */
    private void validateDAG(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        if (nodes == null || edges == null || nodes.size() > 500 || edges.size() > 2000) {
            throw new IllegalArgumentException("节点和连线不能为空，最多500节点和2000连线");
        }
        Map<String, List<String>> links = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (WorkflowNode node : nodes) {
            if (node == null || node.getNodeKey() == null || !node.getNodeKey().matches("[A-Za-z0-9_-]{1,64}")
                    || node.getNodeName() == null || node.getNodeName().isBlank()
                    || node.getNodeType() == null || node.getNodeType().isBlank()
                    || indegree.putIfAbsent(node.getNodeKey(), 0) != null) {
                throw new IllegalArgumentException("节点标识、名称和类型无效或重复");
            }
            try {
                String config = node.getConfigJson() == null ? "{}" : node.getConfigJson();
                if (config.length() > 65536 || !objectMapper.readTree(config).isObject()) {
                    throw new IllegalArgumentException("节点配置需要64KB以内JSON对象");
                }
            } catch (Exception error) {
                throw new IllegalArgumentException("节点配置需要有效JSON对象");
            }
            links.put(node.getNodeKey(), new ArrayList<>());
        }
        Set<String> pairs = new HashSet<>();
        for (WorkflowEdge edge : edges) {
            if (edge == null || !links.containsKey(edge.getSourceNodeKey()) || !links.containsKey(edge.getTargetNodeKey())
                    || !pairs.add(edge.getSourceNodeKey() + "|" + edge.getTargetNodeKey())) {
                throw new IllegalArgumentException("依赖引用无效或重复");
            }
            links.get(edge.getSourceNodeKey()).add(edge.getTargetNodeKey());
            indegree.compute(edge.getTargetNodeKey(), (key, degree) -> degree + 1);
        }
        Queue<String> ready = new ArrayDeque<>();
        indegree.forEach((key, degree) -> { if (degree == 0) ready.add(key); });
        int visited = 0;
        while (!ready.isEmpty()) {
            String key = ready.remove();
            visited++;
            for (String next : links.get(key)) {
                if (indegree.compute(next, (ignored, degree) -> degree - 1) == 0) ready.add(next);
            }
        }
        if (visited != nodes.size()) throw new IllegalArgumentException("工作流存在循环依赖");
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

    /**
     * 根据稳定 UID 获取任务实例，避免 Agent Controller 遍历列表。 English: Read the task by stable UID and reject missing instances without scanning a list.
     *
     * @param uid 任务实例 UID
     * @return 任务实例
     */
    @Override
    public TaskInstance getInstanceByUid(String uid) {
        TaskInstance instance = workflowMapper.findInstanceByUid(uid);
        if (instance == null) {
            throw new IllegalArgumentException("TaskInstance not found: " + uid);
        }
        return instance;
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

    /**
     * 按需读取节点日志，未请求日志时使用排除 log_content 的专用 SQL。 English: Choose the dedicated query without log_content when log summaries are not requested.
     *
     * @param taskInstanceId 任务实例数字 ID
     * @param includeLogSummary 是否读取日志字段
     * @return 节点实例列表
     */
    @Override
    public List<NodeInstance> getNodeInstances(Long taskInstanceId, boolean includeLogSummary) {
        return includeLogSummary
                ? workflowMapper.findNodeInstancesByTaskId(taskInstanceId)
                : workflowMapper.findNodeInstancesWithoutLog(taskInstanceId);
    }
}
