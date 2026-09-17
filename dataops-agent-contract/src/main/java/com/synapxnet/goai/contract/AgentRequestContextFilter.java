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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 校验 Agent 公共 Header、委托身份和工具路径，并在请求结束后清理 MDC。
 * English: Validate or project trusted request context and clear thread-local audit state without exposing credentials.
 */
final class AgentRequestContextFilter extends OncePerRequestFilter {

    private static final Pattern TOOL_PATH = Pattern.compile("^/api/agent/v1/tools/([^/]+):invoke$");
    private final DelegatedTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;

    /**
     * 创建请求上下文过滤器。
     *
     * @param tokenVerifier 短期委托令牌验证器
     * @param objectMapper Spring 统一配置的 JSON 序列化器
 * English: Inject the delegated token verifier and JSON serializer into the request filter.
 */
    AgentRequestContextFilter(DelegatedTokenVerifier tokenVerifier, ObjectMapper objectMapper) {
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
    }

    /**
     * 仅拦截 Agent v1 API，校验上下文后向后传递请求并保证清理线程上下文。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
 * English: Validate Agent v1 request context before forwarding and clear MDC in the finalization path.
 */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/agent/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            try {
                String workspaceId = requiredHeader(request, "X-OpenXnet-Workspace-Id");
                String incidentId = requiredHeader(request, "X-OpenXnet-Incident-Id");
                String traceId = requiredHeader(request, "X-OpenXnet-Trace-Id");
                String toolName = requiredHeader(request, "X-OpenXnet-Tool-Name");
                String idempotencyKey = requiredHeader(request, "Idempotency-Key");
                verifyPathTool(request.getRequestURI(), toolName);
                String actorId = tokenVerifier.verify(request.getHeader("Authorization"), workspaceId, toolName);
                AgentContract.RequestContext context = new AgentContract.RequestContext(
                        workspaceId, incidentId, traceId, toolName, idempotencyKey, actorId, null);
                request.setAttribute(AgentContract.CONTEXT_ATTRIBUTE, context);
                putMdc(context);
            } catch (AgentContractException exception) {
                writeContractError(response, exception);
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 将过滤器阶段的鉴权或契约异常写为脱敏公共 JSON 包络。
     *
     * @param response HTTP 响应
     * @param exception 已识别的公共契约异常
     * @throws IOException 响应写入失败时抛出
 * English: Serialize a sanitized ToolResponse for authentication or contract failures raised by this filter.
 */
    private void writeContractError(
            HttpServletResponse response,
            AgentContractException exception) throws IOException {
        AgentContract.ToolMeta meta = new AgentContract.ToolMeta(
                null, null, null, null, null, AgentContract.CONTRACT_VERSION,
                Instant.now(), 0L, "agent-contract", null, null);
        AgentContract.ToolError error = new AgentContract.ToolError(
                exception.getCode(), exception.getMessage(), exception.isRetryable(), exception.getDetails());
        AgentContract.ToolResponse<Void> body = new AgentContract.ToolResponse<>(
                false, null, error, meta, null);
        response.resetBuffer();
        response.setStatus(exception.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * 读取并限制公共 Header，避免空值和超长日志污染。
     *
     * @param request HTTP 请求
     * @param name Header 名称
     * @return 已校验 Header 值
 * English: Read a required header and reject blank or overlong values.
 */
    private String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank() || value.length() > 256
                || !value.matches("[A-Za-z0-9._:/-]+")) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", name + " 缺失或格式无效");
        }
        return value;
    }

    /**
     * 校验 invoke 路径中的工具名与 Header 一致。
     *
     * @param path 请求路径
     * @param toolName Header 中的工具名
 * English: Require the tool name in the invoke path to match the header.
 */
    private void verifyPathTool(String path, String toolName) {
        Matcher matcher = TOOL_PATH.matcher(path);
        if (matcher.matches() && !matcher.group(1).equals(toolName)) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "路径工具名与请求头不一致");
        }
    }

    /**
     * 将可审计但不含凭据的上下文字段写入 MDC。
     *
     * @param context 已验证上下文
 * English: Write noncredential request identifiers into MDC for audit correlation.
 */
    private void putMdc(AgentContract.RequestContext context) {
        MDC.put("workspaceId", context.workspaceId());
        MDC.put("incidentId", context.incidentId());
        MDC.put("traceId", context.traceId());
        MDC.put("toolName", context.toolName());
        MDC.put("actorId", context.actorId());
    }
}
