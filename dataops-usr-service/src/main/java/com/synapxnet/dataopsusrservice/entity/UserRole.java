package com.synapxnet.dataopsusrservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRole {
    private Long id;
    private Long userId;
    private Long roleId;
    private Long clusterId;
    private LocalDateTime createdAt;

    // joined fields
    private String username;
    private String roleName;
    private String roleCode;
}
