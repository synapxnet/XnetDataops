package com.synapxnet.dataopsdobservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MonitorEvent {
    private Long id;
    private String uid;
    private Long monitorId;
    private String eventType;
    private String eventValue;
    private String expectedValue;
    private String message;
    private String status;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}
