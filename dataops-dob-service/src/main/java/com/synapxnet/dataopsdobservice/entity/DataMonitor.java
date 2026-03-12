package com.synapxnet.dataopsdobservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataMonitor {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String tableName;
    private String monitorType;
    private String checkExpression;
    private String thresholdValue;
    private String alertLevel;
    private Boolean enabled;
    private String scheduleCron;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
