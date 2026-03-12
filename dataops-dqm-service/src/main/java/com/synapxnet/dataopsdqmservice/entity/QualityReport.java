package com.synapxnet.dataopsdqmservice.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QualityReport {
    private Long id;
    private String uid;
    private Long ruleId;
    private LocalDateTime checkTime;
    private String status;
    private Long totalRows;
    private Long failedRows;
    private BigDecimal passRate;
    private String detailJson;
    private LocalDateTime createdAt;
}
