package com.synapxnet.dataopsddvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QueryHistory {
    private Long id;
    private String uid;
    private Long datasourceId;
    private String sqlContent;
    private String executeStatus;
    private Long rowsAffected;
    private Long durationMs;
    private String errorMsg;
    private String executedBy;
    private LocalDateTime executedAt;
}
