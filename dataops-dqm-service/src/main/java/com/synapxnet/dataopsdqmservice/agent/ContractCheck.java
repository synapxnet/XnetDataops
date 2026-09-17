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
package com.synapxnet.dataopsdqmservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示 DataOps 对受控外部契约引用执行的持久化检查事实。
 * English: Represent the documented typed contract without adding implicit domain behavior.
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
