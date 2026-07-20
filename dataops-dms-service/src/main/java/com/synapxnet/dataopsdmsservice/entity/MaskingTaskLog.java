package com.synapxnet.dataopsdmsservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaskingTaskLog {
    private Long id;
    private String uid;
    private Long policyId;
    private String status;
    private Long totalRows;
    private Long maskedRows;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMsg;
    private LocalDateTime createdAt;
}
