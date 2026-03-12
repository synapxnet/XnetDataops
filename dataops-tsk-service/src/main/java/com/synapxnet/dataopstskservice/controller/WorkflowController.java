package com.synapxnet.dataopstskservice.controller;

import com.synapxnet.dataopstskservice.common.Result;
import com.synapxnet.dataopstskservice.entity.*;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tsk")
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) { this.workflowService = workflowService; }

    @GetMapping("/workflows")
    public Result<List<Workflow>> list() { return Result.success(workflowService.listAll()); }

    @GetMapping("/workflows/{id}")
    public Result<Workflow> get(@PathVariable("id") Long id) { return Result.success(workflowService.getById(id)); }

    @PostMapping("/workflows")
    public Result<Workflow> create(@RequestBody Workflow workflow) { return Result.success(workflowService.create(workflow)); }

    @PutMapping("/workflows/{id}")
    public Result<Workflow> update(@PathVariable("id") Long id, @RequestBody Workflow workflow) {
        workflow.setId(id);
        return Result.success(workflowService.update(workflow));
    }

    @DeleteMapping("/workflows/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) { workflowService.delete(id); return Result.success(); }

    @PutMapping("/workflows/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> params) {
        workflowService.updateStatus(id, params.get("status"));
        return Result.success();
    }

    @GetMapping("/workflows/{id}/nodes")
    public Result<List<WorkflowNode>> getNodes(@PathVariable("id") Long id) { return Result.success(workflowService.getNodes(id)); }

    @GetMapping("/workflows/{id}/edges")
    public Result<List<WorkflowEdge>> getEdges(@PathVariable("id") Long id) { return Result.success(workflowService.getEdges(id)); }

    @PutMapping("/workflows/{id}/dag")
    public Result<Void> saveDAG(@PathVariable("id") Long id, @RequestBody Map<String, Object> dagData) {
        @SuppressWarnings("unchecked")
        List<WorkflowNode> nodes = (List<WorkflowNode>) dagData.get("nodes");
        @SuppressWarnings("unchecked")
        List<WorkflowEdge> edges = (List<WorkflowEdge>) dagData.get("edges");
        workflowService.saveDAG(id, nodes, edges);
        return Result.success();
    }

    @PostMapping("/workflows/{id}/trigger")
    public Result<TaskInstance> trigger(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> params) {
        String triggerType = params != null ? params.get("triggerType") : "manual";
        return Result.success(workflowService.triggerWorkflow(id, triggerType));
    }
}
