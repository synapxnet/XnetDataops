/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：验证两种身份与资源隔离。Purpose: Test identity mapping and resource isolation.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.goai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import static org.junit.jupiter.api.Assertions.*;

class DataOpsResourceScopesTest {
    static final String GRANTS = """
      [{"scopeId":"scope-a","workspaceId":"workspace-a","tenantUid":"tenant-a","deptUid":"dept-a","teamUid":"team-a",
      "executionMode":"fixture","assetUids":["asset-a"],"reportUids":["report-a"],"instanceUids":["run-a"]}]
      """;

    /** 网页与委托身份应落到同一明确范围。 Browser and delegated identities must resolve to the same explicit scope. */
    @Test void resolvesSharedScopeAndRejectsForeignResources() {
        DataOpsResourceScopes scopes = new DataOpsResourceScopes(GRANTS, new ObjectMapper());
        var browser = scopes.forOrganization("tenant-a", "dept-a", "team-a");
        assertEquals(browser, scopes.forWorkspace("workspace-a"));
        assertThrows(AgentContractException.class, () -> browser.requireAsset("asset-b"));
        assertThrows(AgentContractException.class, () -> browser.requireReport("asset-a"));
        assertThrows(AgentContractException.class, () -> scopes.forWorkspace("workspace-b"));
        assertThrows(AgentContractException.class, () -> scopes.forOrganization("tenant-b", "dept-a", "team-a"));
    }

    /** 空、错误、歧义及缺模式映射均不授予数据。 Empty, malformed, ambiguous and mode-less grants must fail closed. */
    @Test void rejectsUnsafeConfiguration() {
        for (String config : new String[]{"[]", "broken", GRANTS.replace("fixture", ""), GRANTS.replace("}]", "}," + GRANTS.substring(1))}) {
            var scopes = new DataOpsResourceScopes(config, new ObjectMapper());
            assertEquals(503, assertThrows(AgentContractException.class, () -> scopes.forWorkspace("workspace-a")).getHttpStatus());
        }
    }

    /** 只有USR确认后才解析浏览器范围，拒绝重定向。 Resolve browser grants only after USR confirmation and reject redirects. */
    @Test void verifiesAuthorityWithoutFollowingRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // 本地测试认证源返回受控状态，不访问外部业务。 Return controlled authority responses without external business access.
        server.createContext("/api/usr/organization-access", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("Authorization");
            int status = "Bearer accepted".equals(token) ? 204 : "Bearer redirect".equals(token) ? 302
                    : "Bearer html".equals(token) ? 200 : "Bearer pending".equals(token) ? 202 : 403;
            exchange.getResponseHeaders().add("Location", "/unexpected");
            exchange.sendResponseHeaders(status, -1); exchange.close();
        });
        server.start();
        try {
            var verifier = new DataOpsOrganizationVerifier("http://127.0.0.1:" + server.getAddress().getPort(), new DataOpsResourceScopes(GRANTS, new ObjectMapper()));
            assertEquals("scope-a", verifier.verify("Bearer accepted", "tenant-a", "dept-a", "team-a").scopeId());
            assertEquals(403, assertThrows(AgentContractException.class, () -> verifier.verify("Bearer rejected", "tenant-a", "dept-a", "team-a")).getHttpStatus());
            assertEquals(503, assertThrows(AgentContractException.class, () -> verifier.verify("Bearer redirect", "tenant-a", "dept-a", "team-a")).getHttpStatus());
            assertEquals(503, assertThrows(AgentContractException.class, () -> verifier.verify("Bearer html", "tenant-a", "dept-a", "team-a")).getHttpStatus());
            assertEquals(503, assertThrows(AgentContractException.class, () -> verifier.verify("Bearer pending", "tenant-a", "dept-a", "team-a")).getHttpStatus());
            assertEquals(401, assertThrows(AgentContractException.class, () -> verifier.verify(null, "tenant-a", "dept-a", "team-a")).getHttpStatus());
        } finally { server.stop(0); }
    }
}
