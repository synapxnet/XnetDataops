package com.synapxnet.dataopsddvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SavedQuery {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String sqlContent;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
