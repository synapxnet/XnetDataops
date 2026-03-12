package com.synapxnet.dataopstskservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskInstance {
    private Long id;
    private String uid;
    private Long workflowId;
    private String status;
    private String triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
}
