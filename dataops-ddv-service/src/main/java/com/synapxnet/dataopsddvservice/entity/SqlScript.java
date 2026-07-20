package com.synapxnet.dataopsddvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SqlScript {
    private Long id;
    private String uid;
    private String name;
    private Long datasourceId;
    private String content;
    private String scriptType;
    private String folderPath;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
