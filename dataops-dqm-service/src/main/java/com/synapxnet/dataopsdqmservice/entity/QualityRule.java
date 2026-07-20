package com.synapxnet.dataopsdqmservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QualityRule {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String tableName;
    private String columnName;
    private String ruleType;
    private String ruleExpression;
    private String severity;
    private Boolean enabled;
    private String scheduleCron;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
