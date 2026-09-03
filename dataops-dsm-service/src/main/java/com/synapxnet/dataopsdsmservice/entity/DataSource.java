package com.synapxnet.dataopsdsmservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
public class DataSource {
    private Long id;
    private String uid;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String encryptedPassword;
    private String connectionParams;
    private String status;
    private LocalDateTime lastTestAt;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
