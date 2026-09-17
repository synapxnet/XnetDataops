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
package com.synapxnet.goai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * 自动注册 GOAI Agent 契约的鉴权过滤器和结构化异常处理器。
 * English: Validate or project trusted request context and clear thread-local audit state without exposing credentials.
 */
@AutoConfiguration
public class AgentContractAutoConfiguration {
    /** 注册网页登录验证，独立于委托JWT。 Register browser authority verification separately from delegated JWT validation. */
    @Bean
    DataOpsOrganizationVerifier dataOpsOrganizationVerifier(
            @Value("${openxnet.dataops.usr-base-url:}") String baseUrl, DataOpsResourceScopes scopes) {
        return new DataOpsOrganizationVerifier(baseUrl, scopes);
    }
    /** 创建默认拒绝的资源映射。 Create explicit server-owned grants with no unrestricted fallback. */
    @Bean
    DataOpsResourceScopes dataOpsResourceScopes(
            @Value("${openxnet.dataops.resource-scopes-json:[]}") String json, ObjectMapper mapper) {
        return new DataOpsResourceScopes(json, mapper);
    }

    /**
     * 创建短期委托令牌验证器。
     *
     * @param secret 环境注入的 HMAC 密钥
     * @param audience Adapter 受众
     * @return 失败关闭的令牌验证器
 * English: Register the delegated token verifier from server signing configuration.
 */
    @Bean
    DelegatedTokenVerifier delegatedTokenVerifier(
            @Value("${openxnet.agent.delegation-secret:}") String secret,
            @Value("${openxnet.agent.audience:openxnet-agent-adapter}") String audience) {
        return new DelegatedTokenVerifier(secret, audience);
    }

    /**
     * 创建三平台共用的计划级审批内省客户端。
     *
     * @param objectMapper Spring JSON 映射器
     * @param baseUrl 审批服务根地址
     * @param serviceToken 审批服务内部令牌
     * @param connectTimeoutMillis 审批服务连接超时毫秒数
     * @param readTimeoutMillis 审批服务读取超时毫秒数
     * @return 失败关闭的审批验证器
 * English: Register the shared approval introspection client with configured endpoint and time budgets.
 */
    @Bean
    GovernedApprovalVerifier governedApprovalVerifier(
            ObjectMapper objectMapper,
            @Value("${openxnet.approval.base-url:http://127.0.0.1:8080}") String baseUrl,
            @Value("${openxnet.approval.service-token:}") String serviceToken,
            @Value("${openxnet.approval.connect-timeout-ms:3000}") int connectTimeoutMillis,
            @Value("${openxnet.approval.read-timeout-ms:5000}") int readTimeoutMillis) {
        return new GovernedApprovalVerifier(
                objectMapper, baseUrl, serviceToken, connectTimeoutMillis, readTimeoutMillis);
    }

    /**
     * 注册只作用于 Agent v1 API 的上下文过滤器。
     *
     * @param verifier 委托令牌验证器
     * @param objectMapper Spring 统一配置的 JSON 序列化器
     * @return Servlet 过滤器注册对象
 * English: Register the context filter for Agent v1 API requests.
 */
    @Bean
    FilterRegistrationBean<AgentRequestContextFilter> agentRequestContextFilter(
            DelegatedTokenVerifier verifier,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<AgentRequestContextFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AgentRequestContextFilter(verifier, objectMapper));
        bean.addUrlPatterns("/api/agent/v1/*");
        bean.setOrder(-100);
        return bean;
    }

    /**
     * 注册公共契约异常处理器。
     *
     * @return 结构化异常处理器
 * English: Register the shared Agent contract exception advice.
 */
    @Bean
    AgentExceptionHandler agentExceptionHandler() {
        return new AgentExceptionHandler();
    }
}
