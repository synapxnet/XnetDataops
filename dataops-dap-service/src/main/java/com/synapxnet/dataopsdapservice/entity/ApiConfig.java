package com.synapxnet.dataopsdapservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiConfig {
    private Long id;
    private String uid;
    private String name;
    private String path;
    private String method;
    private Long datasourceId;
    private String sqlContent;
    private String paramConfig;
    private String description;
    private String status;
    private Integer rateLimit;
    private Integer cacheTtl;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
