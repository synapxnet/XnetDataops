package com.synapxnet.dataopsdasservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssetClassification {
    private Long id;
    private String name;
    private String level;
    private Long parentId;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
