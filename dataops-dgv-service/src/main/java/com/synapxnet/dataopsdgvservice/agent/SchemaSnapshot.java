package com.synapxnet.dataopsdgvservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示持久化的 Schema 快照记录；只保存字段契约，不保存数据样本。
 */
@Data
public class SchemaSnapshot {
    private Long id;
    private String uid;
    private String assetUid;
    private String assetName;
    private String schemaVersion;
    private Integer fieldCount;
    private String schemaHash;
    private String schemaJson;
    private String sourceType;
    private LocalDateTime capturedAt;
    private String createdBy;
}
