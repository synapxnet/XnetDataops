package com.synapxnet.dataopsdapservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiCallLog {
    private Long id;
    private Long apiConfigId;
    private Long apiKeyId;
    private String requestParams;
    private Integer responseStatus;
    private Long responseTime;
    private String ipAddress;
    private LocalDateTime calledAt;
}
