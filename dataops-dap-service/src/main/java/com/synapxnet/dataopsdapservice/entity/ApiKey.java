package com.synapxnet.dataopsdapservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiKey {
    private Long id;
    private String uid;
    private String appName;
    private String apiKey;
    private String secretKey;
    private String status;
    private String permissions;
    private LocalDateTime expireAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
