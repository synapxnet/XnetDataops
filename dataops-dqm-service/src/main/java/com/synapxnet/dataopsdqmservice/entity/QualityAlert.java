package com.synapxnet.dataopsdqmservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QualityAlert {
    private Long id;
    private String uid;
    private Long ruleId;
    private Long reportId;
    private String alertLevel;
    private String message;
    private String status;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
}
