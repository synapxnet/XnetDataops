package com.synapxnet.dataopsdimservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SyncLog {
    private Long id;
    private String uid;
    private Long taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Long rowsRead;
    private Long rowsWritten;
    private String errorMsg;
    private LocalDateTime createdAt;
}
