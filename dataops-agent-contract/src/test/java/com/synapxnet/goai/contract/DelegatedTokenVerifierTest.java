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
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证 DataOps Adapter 的委托 Token 边界。
 * English: Verify delegated-token boundaries, including signature, lifetime, audience and scoped tool authorization.
 */
class DelegatedTokenVerifierTest {

    private static final String SECRET = "goai-dataops-delegation-test-secret-123456";

    /** 正确受众、Workspace 和工具应返回可信主体。
 * English: Apply the documented typed evidence operation and preserve its explicit input, output and failure boundaries.
 */
    @Test
    void acceptsExactDelegation() throws Exception {
        DelegatedTokenVerifier verifier = new DelegatedTokenVerifier(SECRET, "openxnet-agent-adapter");
        String token = token("ws_goai_demo", List.of("dataops.schema.snapshot.get"), 60);

        assertEquals("investigator", verifier.verify(
                "Bearer " + token, "ws_goai_demo", "dataops.schema.snapshot.get"));
    }

    /** Token 不能横向复用到其他 Workspace 或工具。
 * English: Verify delegated-token boundaries, including signature, lifetime, audience and scoped tool authorization.
 */
    @Test
    void rejectsWorkspaceAndToolReuse() throws Exception {
        DelegatedTokenVerifier verifier = new DelegatedTokenVerifier(SECRET, "openxnet-agent-adapter");
        String token = "Bearer " + token("ws_goai_demo", List.of("dataops.schema.snapshot.get"), 60);

        assertEquals("PERMISSION_DENIED", assertThrows(
                AgentContractException.class,
                () -> verifier.verify(token, "ws_other", "dataops.schema.snapshot.get")).getCode());
        assertEquals("PERMISSION_DENIED", assertThrows(
                AgentContractException.class,
                () -> verifier.verify(token, "ws_goai_demo", "dataops.lineage.get")).getCode());
    }

    /** 超过五分钟的委托 Token 必须失败关闭。
 * English: Verify delegated-token boundaries, including signature, lifetime, audience and scoped tool authorization.
 */
    @Test
    void rejectsLongLivedDelegation() throws Exception {
        DelegatedTokenVerifier verifier = new DelegatedTokenVerifier(SECRET, "openxnet-agent-adapter");
        String token = "Bearer " + token("ws_goai_demo", List.of("dataops.schema.snapshot.get"), 3600);

        assertEquals("UNAUTHENTICATED", assertThrows(
                AgentContractException.class,
                () -> verifier.verify(token, "ws_goai_demo", "dataops.schema.snapshot.get")).getCode());
    }

    /** 生成仅用于测试的 HS256 委托 Token。
 * English: Generate a test-only HS256 token with the requested workspace, tools and lifetime.
 */
    private String token(String workspaceId, List<String> tools, long lifetimeSeconds) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String header = encode(mapper, Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encode(mapper, Map.of(
                "sub", "investigator", "workspace_id", workspaceId,
                "aud", "openxnet-agent-adapter", "tools", tools,
                "exp", Instant.now().getEpochSecond() + lifetimeSeconds));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return header + "." + payload + "." + signature;
    }

    /** 编码一个测试 JWT JSON 段。
 * English: Serialize and Base64URL-encode one test JWT segment.
 */
    private String encode(ObjectMapper mapper, Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(value));
    }
}
