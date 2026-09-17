/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：API密钥服务端授权和安全响应。Purpose: Server authorization and safe API key responses.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopsdapservice.dto;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import java.time.LocalDateTime;

public record ApiKeySummary(Long id, String uid, String appName, String apiKey, String status,
        String permissions, LocalDateTime expireAt, String createdBy, LocalDateTime createdAt) {
    /** 投影列表公开字段，完全省略secretKey且不改动原实体。 Project list fields without secretKey while leaving the original entity untouched. */
    public static ApiKeySummary from(ApiKey key) {
        String value=key.getApiKey();
        String masked=value==null||value.isBlank()?"—":value.length()<=8?"••••":value.substring(0,4)+"••••"+value.substring(value.length()-4);
        return new ApiKeySummary(key.getId(),key.getUid(),key.getAppName(),masked,key.getStatus(),key.getPermissions(),key.getExpireAt(),key.getCreatedBy(),key.getCreatedAt());
    }
}
