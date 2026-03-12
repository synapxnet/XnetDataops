package com.synapxnet.dataopsdmsservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaskingRule {
    private Long id;
    private String uid;
    private String name;
    private String ruleType;
    private String maskPattern;
    private String replacement;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
