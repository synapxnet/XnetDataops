/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：恢复并约束原生只读取证契约。Purpose: Restore bounded native evidence contracts.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
Historical SynapXnet source restored selectively; existing repository license retained.
 */
package com.synapxnet.dataopsdgvservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示持久化的 Schema 快照记录；只保存字段契约，不保存数据样本。
 * English: Read or verify typed schema evidence from persisted snapshots while excluding sample values.
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
