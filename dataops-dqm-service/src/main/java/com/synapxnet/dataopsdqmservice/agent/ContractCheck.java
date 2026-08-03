package com.synapxnet.dataopsdqmservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示 DataOps 对受控外部契约引用执行的持久化检查事实。
 */
@Data
public class ContractCheck {
    private Long id;
    private String uid;
    private String reportUid;
    private String assetUid;
    private String schemaSnapshotUid;
    private String contractRef;
    private Integer expectedFieldCount;
    private Integer actualFieldCount;
    private String status;
    private String mismatchJson;
    private LocalDateTime checkedAt;
    private String incidentId;
    private String traceId;
}
