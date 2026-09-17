/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：验证网页用户组织身份。Purpose: Verify browser identity through the trusted USR authority.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.goai.contract;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class DataOpsOrganizationVerifier {
    private final URI endpoint;
    private final DataOpsResourceScopes scopes;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    /** 固定服务端认证地址，不接受浏览器URL。 Pin the server-configured authority and reject client-selected URLs. */
    public DataOpsOrganizationVerifier(String baseUrl, DataOpsResourceScopes scopes) {
        URI value = null;
        try {
            URI base = URI.create(baseUrl);
            if (("http".equals(base.getScheme()) || "https".equals(base.getScheme())) && base.getHost() != null
                    && base.getUserInfo() == null && base.getQuery() == null && base.getFragment() == null) {
                value = base.resolve("/api/usr/organization-access");
            }
        } catch (IllegalArgumentException ignored) { /* 配置无效时拒绝。 Reject invalid configuration. */ }
        endpoint = value;
        this.scopes = scopes;
    }

    /** 验证登录、组织权限后读取同一资源映射。 Verify login and membership before resolving the shared resource scope. */
    public DataOpsResourceScopes.Scope verify(String authorization, String tenant, String department, String team) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 8192) {
            throw new AgentContractException(401, "UNAUTHENTICATED", "需要有效的登录凭据");
        }
        for (String value : new String[]{tenant, department, team}) {
            if (value == null || !value.matches("[A-Za-z0-9._:-]{1,128}")) {
                throw new AgentContractException(400, "SCOPE_REQUIRED", "请选择完整的组织范围");
            }
        }
        if (endpoint == null) throw new AgentContractException(503, "AUTHORITY_UNAVAILABLE", "组织身份服务尚未配置");
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(3))
                    .header("Authorization", authorization).header("X-Tenant-Uid", tenant)
                    .header("X-Dept-Uid", department).header("X-Team-Uid", team).GET().build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 401) throw new AgentContractException(401, "UNAUTHENTICATED", "登录已失效");
            if (status == 403) throw new AgentContractException(403, "SCOPE_FORBIDDEN", "没有当前组织的数据权限");
            if (status != 204) throw new AgentContractException(503, "AUTHORITY_UNAVAILABLE", "身份服务未返回有效的授权确认");
        } catch (AgentContractException exception) { throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentContractException(503, "AUTHORITY_UNAVAILABLE", "组织身份验证已中断");
        } catch (Exception exception) {
            throw new AgentContractException(503, "AUTHORITY_UNAVAILABLE", "暂时无法验证组织权限");
        }
        return scopes.forOrganization(tenant, department, team);
    }
}
