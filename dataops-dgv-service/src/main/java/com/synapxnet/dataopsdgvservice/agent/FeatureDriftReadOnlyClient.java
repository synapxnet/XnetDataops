/* Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * DGV 只读真实证据桥接 / DGV read-only actual evidence bridge.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-18
 * Version: 1.3.0 | Security Level: INTERNAL
 * __version__: 1.3.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/** 固定两项只读取证，不加载审批/变更执行类型。 / Read two fixed evidence tools without loading approval or mutation types. */
public final class FeatureDriftReadOnlyClient {
    private static final String SOURCE_MODE = "REAL_CPU_SYNTHETIC_STAGING";
    private static final Set<String> READS = Set.of("dataops.schema.snapshot.get", "dataops.lineage.get");
    private final boolean enabled;
    private final URI base;
    private final String token;
    private final ObjectMapper mapper;
    private final HttpClient http;

    /** 从固定配置与保护文件创建只读连接。 / Create a read-only connection from fixed settings and a protected credential file. */
    public FeatureDriftReadOnlyClient(boolean enabled, String url, String tokenFile, ObjectMapper mapper) {
        this.enabled = enabled;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
        if (enabled) {
            URI candidate = URI.create(url);
            if (!Set.of("http", "https").contains(candidate.getScheme()) || candidate.getHost() == null
                    || candidate.getRawUserInfo() != null || candidate.getRawQuery() != null || candidate.getRawFragment() != null
                    || !(candidate.getPath().isEmpty() || candidate.getPath().equals("/"))) {
                throw new IllegalArgumentException("Invalid fixed read-only feature-drift runtime address");
            }
            this.base = candidate;
            this.token = readToken(tokenFile);
        } else {
            this.base = null;
            this.token = "";
        }
    }

    /** 仅接受明确资产和两项白名单读工具。 / Accept only the exact asset and two allowlisted read tools. */
    public boolean handles(String tool, Object arguments) {
        if (!enabled || !READS.contains(tool == null ? "" : tool) || arguments == null) return false;
        Map<String, Object> values = mapper.convertValue(arguments, new TypeReference<Map<String, Object>>() { });
        return "asset_risk_features_prod".equals(values.get("assetUid"));
    }

    /** 保留已认证上下文并核对真实证据的完整身份。 / Retain authenticated context and verify the complete identity of actual evidence. */
    @SuppressWarnings("unchecked")
    public <T> AgentContract.ToolResponse<T> invoke(AgentContract.ToolRequest<?> body, AgentContract.RequestContext context) {
        long started = System.nanoTime();
        if (!handles(context.toolName(), body.arguments())) throw error(403, "FEATURE_DRIFT_TARGET_FORBIDDEN");
        requireId(context.workspaceId()); requireId(context.incidentId()); requireId(context.traceId()); requireId(context.requestId());
        String endpoint = "/v1/incidents/" + encode(context.incidentId()) + "/evidence?workspaceId=" + encode(context.workspaceId())
            + "&traceId=" + encode(context.traceId()) + "&requestId=" + encode(context.requestId()) + "&toolName=" + encode(context.toolName());
        Map<String, Object> envelope = read(base.resolve(endpoint));
        if (!(envelope.get("data") instanceof Map<?, ?> raw)) throw error(502, "FEATURE_DRIFT_RESPONSE_INVALID");
        Map<String, Object> data = (Map<String, Object>) raw;
        if (!SOURCE_MODE.equals(data.get("sourceMode")) || !Boolean.TRUE.equals(data.get("synthetic"))
                || !context.workspaceId().equals(data.get("workspaceId")) || !context.incidentId().equals(data.get("incidentId"))
                || !context.traceId().equals(data.get("traceId")) || !"asset_risk_features_prod".equals(data.get("assetUid"))) {
            throw error(502, "FEATURE_DRIFT_SCOPE_MISMATCH");
        }
        Object rawVersion = envelope.get("resourceVersion");
        String version = rawVersion == null ? "" : String.valueOf(rawVersion);
        if (!version.matches("[0-9]{1,18}")) throw error(502, "FEATURE_DRIFT_VERSION_MISSING");
        return AgentContract.success((T) data, context, "XnetDataops/feature-drift-runtime", version, started);
    }

    /** 仅发送GET，拒绝重定向、过大响应与运行时错误。 / Send only GET and reject redirects, excessive responses, and runtime errors. */
    private Map<String, Object> read(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
                .header("X-Feature-Drift-Token", token).timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<java.io.InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            byte[] bytes;
            try (java.io.InputStream input = response.body()) { bytes = input.readNBytes(2 * 1024 * 1024 + 1); }
            if (bytes.length > 2 * 1024 * 1024) throw error(502, "FEATURE_DRIFT_RESPONSE_TOO_LARGE");
            if (response.statusCode() != 200) {
                int status = Set.of(400, 403, 404, 409, 412, 422).contains(response.statusCode()) ? response.statusCode() : 502;
                throw error(status, "FEATURE_DRIFT_RUNTIME_REJECTED");
            }
            return mapper.readValue(bytes, new TypeReference<Map<String, Object>>() { });
        } catch (AgentContractException known) { throw known; }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw error(503, "FEATURE_DRIFT_RUNTIME_INTERRUPTED"); }
        catch (Exception unavailable) { throw error(503, "FEATURE_DRIFT_RUNTIME_UNAVAILABLE"); }
    }

    /** 读取有界角色令牌，异常不回显文件或凭据。 / Read a bounded role token without exposing files or credentials on error. */
    private static String readToken(String value) {
        try {
            Path path = Path.of(value);
            if (!path.isAbsolute() || !Files.isRegularFile(path) || Files.size(path) > 8192) throw new IllegalArgumentException();
            String token = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (token.length() < 32 || token.contains("\n") || token.contains("\r")) throw new IllegalArgumentException();
            return token;
        } catch (Exception invalid) { throw new IllegalStateException("Read-only feature-drift role credential unavailable"); }
    }

    /** 限定路径和查询的身份格式。 / Bound identity formats used in paths and query parameters. */
    private static void requireId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,160}")) throw error(400, "FEATURE_DRIFT_ID_INVALID");
    }

    /** 编码单个路径或查询值。 / Encode a single path or query value. */
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    /** 使用既有错误契约且不回退旧证据。 / Use the existing error contract without falling back to old evidence. */
    private static AgentContractException error(int status, String code) {
        return new AgentContractException(status, code, "真实特征漂移只读取证未完成；未回退演练状态。");
    }
}
