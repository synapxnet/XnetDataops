package com.synapxnet.dataopsdgvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TableTag {
    private Long id;
    private Long metaTableId;
    private Long tagId;
    private LocalDateTime createdAt;
}
