package com.synapxnet.dataopsdgvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MetaTable {
    private Long id;
    private String uid;
    private Long datasourceId;
    private String schemaName;
    private String tableName;
    private String tableType;
    private Long rowCount;
    private Long dataSizeBytes;
    private String description;
    private String owner;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
