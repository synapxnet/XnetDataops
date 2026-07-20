package com.synapxnet.dataopsdgvservice.entity;

import lombok.Data;

@Data
public class MetaColumn {
    private Long id;
    private Long metaTableId;
    private String columnName;
    private String columnType;
    private Boolean isNullable;
    private Boolean isPrimaryKey;
    private String defaultValue;
    private String description;
    private Integer sortOrder;
}
