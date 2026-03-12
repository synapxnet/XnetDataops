package com.synapxnet.dataopsdimservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncTask {
    private Long id;
    private String uid;
    private String name;
    private Long sourceDsId;
    private Long targetDsId;
    private String sourceTable;
    private String targetTable;
    private String syncMode;
    private String incrementalField;
    private String scheduleCron;
    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
