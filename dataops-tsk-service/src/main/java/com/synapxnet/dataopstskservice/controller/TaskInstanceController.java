package com.synapxnet.dataopstskservice.controller;

import com.synapxnet.dataopstskservice.common.Result;
import com.synapxnet.dataopstskservice.entity.TaskInstance;
import com.synapxnet.dataopstskservice.entity.NodeInstance;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tsk/instances")
public class TaskInstanceController {
    private final WorkflowService workflowService;

    public TaskInstanceController(WorkflowService workflowService) { this.workflowService = workflowService; }

    @GetMapping
    public Result<List<TaskInstance>> list(@RequestParam(required = false) Long workflowId) {
        return Result.success(workflowService.listInstances(workflowId));
    }

    @GetMapping("/{id}")
    public Result<TaskInstance> get(@PathVariable("id") Long id) { return Result.success(workflowService.getInstance(id)); }

    @GetMapping("/{id}/nodes")
    public Result<List<NodeInstance>> getNodeInstances(@PathVariable("id") Long id) {
        return Result.success(workflowService.getNodeInstances(id));
    }
}
