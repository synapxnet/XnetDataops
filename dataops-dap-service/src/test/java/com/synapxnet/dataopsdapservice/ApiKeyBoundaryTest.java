/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：API密钥服务端授权和安全响应。Purpose: Server authorization and safe API key responses.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopsdapservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.synapxnet.dataopsdapservice.dto.ApiKeySummary;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import com.synapxnet.dataopsdapservice.security.ApiKeyAdministratorVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ApiKeyBoundaryTest {
    private final ObjectMapper mapper=new ObjectMapper();
    /** 缺少登录和缺少可信服务配置时禁止继续。 Stop when authentication or trusted-service configuration is missing. */
    @Test void missingAuthenticationOrConfigurationFailsClosed() {
        var verifier=new ApiKeyAdministratorVerifier("",mapper);
        assertEquals(401,assertThrows(ResponseStatusException.class,()->verifier.requireAdministrator(null)).getStatusCode().value());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->verifier.requireAdministrator("Bearer fixture")).getStatusCode().value());
    }
    /** 仅可信服务认证的管理员可通过，不接受普通用户。 Only trusted-service authenticated administrators pass; ordinary users are rejected. */
    @Test void validatesTrustedIdentityAndAdminRole() throws Exception {
        var response=new AtomicReference<>("{\"code\":0,\"data\":{\"username\":\"fixture.admin\",\"roles\":[\"ADMIN\"]}}");
        var received=new AtomicReference<String>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/usr/user/info",exchange->{received.set(exchange.getRequestHeaders().getFirst("Authorization"));byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});
        server.start();
        try {
            var verifier=new ApiKeyAdministratorVerifier("http://127.0.0.1:"+server.getAddress().getPort(),mapper);
            assertEquals("fixture.admin",verifier.requireAdministrator("Bearer fixture"));
            assertEquals("Bearer fixture",received.get());
            response.set("{\"code\":0,\"data\":{\"username\":\"fixture.user\",\"roles\":[\"USER\"]}}");
            assertEquals(403,assertThrows(ResponseStatusException.class,()->verifier.requireAdministrator("Bearer fixture")).getStatusCode().value());
        } finally { server.stop(0); }
    }
    /** 非200响应和重定向永远不构成授权成功。 Non-200 responses and redirects never constitute successful authorization. */
    @ParameterizedTest @ValueSource(ints={202,302,401,403,500})
    void rejectsRedirectsAndFailedIdentityResponses(int status) throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/usr/user/info",exchange->{exchange.getResponseHeaders().set("Location","http://127.0.0.1:1/never-follow");exchange.sendResponseHeaders(status,-1);exchange.close();});server.start();
        try {var verifier=new ApiKeyAdministratorVerifier("http://127.0.0.1:"+server.getAddress().getPort(),mapper);assertEquals(status==401||status==403?status:503,assertThrows(ResponseStatusException.class,()->verifier.requireAdministrator("Bearer fixture")).getStatusCode().value());}finally{server.stop(0);}
    }
    /** 序列化列表时绝不携带secretKey，短key也必须掩码。 List serialization never includes secretKey and masks even short keys. */
    @Test void summaryOmitsSecretsWithoutMutatingEntity() throws Exception {
        ApiKey key=new ApiKey();key.setApiKey("1234567890abcdef");key.setSecretKey("test-secret-not-a-real-key");
        String json=mapper.writeValueAsString(ApiKeySummary.from(key));
        assertFalse(json.contains("secretKey"));assertFalse(json.contains("test-secret"));assertFalse(json.contains("1234567890abcdef"));
        assertEquals("1234567890abcdef",key.getApiKey());assertEquals("test-secret-not-a-real-key",key.getSecretKey());
        key.setApiKey("1234");assertEquals("••••",ApiKeySummary.from(key).apiKey());
    }
}
