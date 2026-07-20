package com.synapxnet.dataopsdimservice.entity;

import lombok.Data;

@Data
public class FieldMapping {
    private Long id;
    private Long taskId;
    private String sourceField;
    private String targetField;
    private String transformExpression;
    private Integer sortOrder;
}
