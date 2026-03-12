package com.synapxnet.dataopsdgvservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataTag {
    private Long id;
    private String name;
    private String tagType;
    private String color;
    private String description;
    private LocalDateTime createdAt;
}
