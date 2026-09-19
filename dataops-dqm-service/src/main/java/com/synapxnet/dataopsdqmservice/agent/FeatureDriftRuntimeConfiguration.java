/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 真实特征漂移角色配置 / Real feature-drift role configuration.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-18
 * Version: 1.3.0 | Security Level: INTERNAL
 * __version__: 1.3.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdqmservice.agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.FeatureDriftRuntimeClient;
import com.synapxnet.goai.contract.GovernedApprovalVerifier;
import com.synapxnet.goai.contract.DataOpsResourceScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** 在业务扫描包注册独立客户端，不替换既有契约配置。 / Register in the service package without replacing existing contract configuration. */
@Configuration
public class FeatureDriftRuntimeConfiguration {
    /** 原部署缺失范围 Bean 时补充明确授权映射，空配置保持拒绝。 / Supply explicit grants when the original deployment lacks the bean; empty configuration remains denied. */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(DataOpsResourceScopes.class)
    DataOpsResourceScopes featureDriftDataOpsResourceScopes(ObjectMapper mapper,
            @Value("${openxnet.dataops.resource-scopes-json:[]}") String json) {
        return new DataOpsResourceScopes(json, mapper);
    }

    /** 固定平台角色，令牌由受保护文件读取。 / Fix the platform role and read its token from a protected file. */
    @Bean
    FeatureDriftRuntimeClient featureDriftRuntimeClient(ObjectMapper mapper, GovernedApprovalVerifier verifier,
            @Value("${openxnet.feature-drift.enabled:false}") boolean enabled,
            @Value("${openxnet.feature-drift.runtime-url:}") String url,
            @Value("${openxnet.feature-drift.token-file:}") String tokenFile) {
        return new FeatureDriftRuntimeClient(enabled, url, tokenFile, "dataops", mapper, verifier);
    }
}
