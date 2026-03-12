package com.synapxnet.dataopstskservice.entity;

import lombok.Data;

@Data
public class WorkflowNode {
    private Long id;
    private Long workflowId;
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String configJson;
    private Double positionX;
    private Double positionY;
}
