package com.synapxnet.dataopsdauservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataChangeRecord {
    private Long id;
    private String uid;
    private Long datasourceId;
    private String tableName;
    private String changeType;
    private Integer affectedRows;
    private String changeSql;
    private String changedBy;
    private LocalDateTime changedAt;
}
