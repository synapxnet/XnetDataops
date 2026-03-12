package com.synapxnet.dataopsdmsservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaskingPolicy {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String tableName;
    private String columnName;
    private Long ruleId;
    private Boolean enabled;
    private Integer priority;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
