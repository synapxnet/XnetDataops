package com.synapxnet.dataopsdauservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplianceReport {
    private Long id;
    private String uid;
    private String name;
    private String reportType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Integer totalEvents;
    private Integer riskEvents;
    private String status;
    private String generatedBy;
    private LocalDateTime createdAt;
}
