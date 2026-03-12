package com.synapxnet.dataopsdasservice.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DataAsset {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String tableName;
    private String assetType;
    private String category;
    private String domain;
    private String owner;
    private String description;
    private String status;
    private String accessLevel;
    private Long rowCount;
    private Long dataSizeBytes;
    private BigDecimal qualityScore;
    private LocalDateTime lastProfiledAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
