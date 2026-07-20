package com.synapxnet.dataopstskservice.entity;

import lombok.Data;

@Data
public class WorkflowEdge {
    private Long id;
    private Long workflowId;
    private String sourceNodeKey;
    private String targetNodeKey;
}
