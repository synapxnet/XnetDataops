package com.synapxnet.dataopsdauservice.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 接收受信服务写入的 DataOps 本地审计回执镜像，不暴露为模型工具。
 */
@RestController
public class AgentAuditReceiptController {

    private final AgentAuditReceiptService receiptService;
    private final byte[] internalToken;

    /**
     * 创建内部审计入口；未配置服务令牌时保持失败关闭。
     *
     * @param receiptService 审计镜像服务
     * @param internalToken 环境注入的内部服务令牌
     */
    public AgentAuditReceiptController(
            AgentAuditReceiptService receiptService,
            @Value("${openxnet.internal.audit-token:}") String internalToken) {
        this.receiptService = receiptService;
        this.internalToken = internalToken.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 幂等保存审计回执，Authorization 只用于常量时间验证且不会进入日志。
     *
     * @param authorization 内部 Bearer 服务令牌
     * @param request 强类型回执请求
     * @return 已保存回执的稳定编号
     */
    @PostMapping("/api/internal/agent/audit-receipts")
    public ResponseEntity<Map<String, String>> save(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AgentAuditReceiptService.AuditReceiptRequest request) {
        verifyInternalToken(authorization);
        AgentAuditReceiptMirror value = receiptService.save(request);
        return ResponseEntity.ok(Map.of("receiptId", value.getReceiptId(), "status", "RECORDED"));
    }

    /**
     * 使用常量时间比较验证内部服务令牌，配置缺失时失败关闭。
     *
     * @param authorization Authorization Header
     */
    private void verifyInternalToken(String authorization) {
        if (internalToken.length < 32 || authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InternalAuthenticationException();
        }
        byte[] candidate = authorization.substring(7).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(internalToken, candidate)) {
            throw new InternalAuthenticationException();
        }
    }

    /** 将内部身份失败映射为 401，响应不泄露配置状态。 */
    @ExceptionHandler(InternalAuthenticationException.class)
    ResponseEntity<Map<String, String>> handleAuthentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "UNAUTHENTICATED", "message", "内部服务身份无效"));
    }

    /** 将幂等摘要冲突映射为 409。 */
    @ExceptionHandler(AgentAuditReceiptService.AuditReceiptConflictException.class)
    ResponseEntity<Map<String, String>> handleConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "IDEMPOTENCY_CONFLICT", "message", "审计回执摘要冲突"));
    }

    /** 表示内部服务身份无效。 */
    private static final class InternalAuthenticationException extends RuntimeException {
        /** 创建不含凭据内容的内部身份异常。 */
        private InternalAuthenticationException() {
            super("internal authentication failed");
        }
    }
}
