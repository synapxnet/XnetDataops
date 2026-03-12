package com.synapxnet.dataopsdasservice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssetAccessRecord {
    private Long id;
    private String uid;
    private Long assetId;
    private String userId;
    private String accessType;
    private LocalDateTime accessTime;
    private String ipAddress;
    private String detail;
}
