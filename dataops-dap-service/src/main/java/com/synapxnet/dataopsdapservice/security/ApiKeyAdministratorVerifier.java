/* Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：API密钥服务端授权和安全响应。Purpose: Server authorization and safe API key responses.
Author: maoyo | Department: 研发部 | Date: 2026-09-13 | Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com */
package com.synapxnet.dataopsdapservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ApiKeyAdministratorVerifier {
    private final String usrBaseUrl;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    /** 注入服务端配置的可信USR地址，默认不开放密钥管理。 Inject the server-configured trusted USR URL and keep key management closed by default. */
    public ApiKeyAdministratorVerifier(@Value("${openxnet.dataops.usr-base-url:}") String usrBaseUrl, ObjectMapper mapper) {
        this.usrBaseUrl = usrBaseUrl;
        this.mapper = mapper;
    }

    /** 向可信USR验证Bearer身份和管理员角色，不接受客户端角色自报。 Verify bearer identity and administrator role with trusted USR rather than client-supplied roles. */
    public String requireAdministrator(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() < 8 || authorization.length() > 8192) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (usrBaseUrl == null || usrBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "密钥管理尚未配置可信身份服务");
        }
        try {
            URI base = URI.create(usrBaseUrl);
            if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme())) || base.getHost() == null
                    || base.getUserInfo() != null || base.getRawQuery() != null || base.getFragment() != null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "可信身份服务配置无效");
            }
            URI endpoint = URI.create(usrBaseUrl.replaceAll("/+$", "") + "/api/usr/user/info");
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(4))
                    .header("Authorization", authorization).header("Accept", "application/json").GET().build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var stream = response.body()) {
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()), "当前用户无权管理API密钥");
                }
                if (response.statusCode() != 200) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用");
                byte[] bytes = stream.readNBytes(65537);
                if (bytes.length > 65536) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份响应无效");
                JsonNode envelope = mapper.readTree(bytes);
                JsonNode user = envelope.path("data");
                if (!envelope.path("code").isInt() || envelope.path("code").asInt() != 0 || !user.isObject()) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份响应无效");
                }
                boolean administrator = "admin".equalsIgnoreCase(user.path("userType").asText());
                for (JsonNode role : user.path("roles")) administrator |= "ADMIN".equalsIgnoreCase(role.asText());
                if (!administrator) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "密钥管理需要管理员权限");
                String username = user.path("username").asText("");
                if (username.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份响应缺少用户标识");
                return username;
            }
        } catch (ResponseStatusException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务请求中断");
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用");
        }
    }
}
