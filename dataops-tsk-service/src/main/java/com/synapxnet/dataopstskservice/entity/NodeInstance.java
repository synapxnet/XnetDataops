package com.synapxnet.dataopstskservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeInstance {
    private Long id;
    private Long taskInstanceId;
    private String nodeKey;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String logContent;
}
