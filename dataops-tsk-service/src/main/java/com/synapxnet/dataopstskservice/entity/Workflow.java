package com.synapxnet.dataopstskservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Workflow {
    private Long id;
    private String uid;
    private String name;
    private String description;
    private String scheduleCron;
    private String status;
    private String dagJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
