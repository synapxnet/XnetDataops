package com.synapxnet.dataopsdauservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private String uid;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String targetName;
    private String detail;
    private String ipAddress;
    private LocalDateTime operateAt;
}
