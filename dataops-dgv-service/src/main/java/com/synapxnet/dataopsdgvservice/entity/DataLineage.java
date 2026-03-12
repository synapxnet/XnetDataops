package com.synapxnet.dataopsdgvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataLineage {
    private Long id;
    private String uid;
    private Long sourceTableId;
    private Long targetTableId;
    private String transformType;
    private String relationshipDesc;
    private Long workflowId;
    private LocalDateTime createdAt;
}
