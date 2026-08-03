package com.synapxnet.dataopsdauservice.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * 以 receiptId 幂等保存 DataOps 本地审计镜像并拒绝摘要冲突。
 */
@Service
public class AgentAuditReceiptService {

    private final AgentAuditReceiptMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建审计镜像服务。
     *
     * @param mapper 追加式回执 Mapper
     * @param objectMapper Jackson 序列化器
     */
    public AgentAuditReceiptService(AgentAuditReceiptMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存或返回完全相同的已有回执；同 UID 不同摘要时拒绝。
     *
     * @param request 已校验回执请求
     * @return 持久化回执镜像
     */
    public AgentAuditReceiptMirror save(AuditReceiptRequest request) {
        requireRequest(request);
        AgentAuditReceiptMirror existing = mapper.findByReceiptId(request.receiptId());
        if (existing != null) {
            if (!existing.getRequestDigest().equals(request.requestDigest())
                    || !existing.getResultDigest().equals(request.resultDigest())) {
                throw new AuditReceiptConflictException("相同 receiptId 对应不同摘要");
            }
            return existing;
        }
        AgentAuditReceiptMirror receipt = toEntity(request);
        mapper.insert(receipt);
        return mapper.findByReceiptId(request.receiptId());
    }

    /** 校验必填字段和 SHA-256 格式。 */
    private void requireRequest(AuditReceiptRequest request) {
        if (request == null || blank(request.receiptId()) || blank(request.requestId())
                || blank(request.workspaceId()) || blank(request.incidentId()) || blank(request.traceId())
                || blank(request.toolName()) || blank(request.actorId()) || blank(request.actionStatus())
                || request.payload() == null || !sha256(request.requestDigest()) || !sha256(request.resultDigest())) {
            throw new IllegalArgumentException("审计回执字段缺失或摘要格式无效");
        }
    }

    /** 将强类型请求转换为持久化 Entity。 */
    private AgentAuditReceiptMirror toEntity(AuditReceiptRequest request) {
        try {
            AgentAuditReceiptMirror value = new AgentAuditReceiptMirror();
            value.setReceiptId(request.receiptId());
            value.setRequestId(request.requestId());
            value.setWorkspaceId(request.workspaceId());
            value.setIncidentId(request.incidentId());
            value.setTraceId(request.traceId());
            value.setToolName(request.toolName());
            value.setActorId(request.actorId());
            value.setApprovalId(request.approvalId());
            value.setRequestDigest(request.requestDigest());
            value.setResultDigest(request.resultDigest());
            value.setActionStatus(request.actionStatus());
            value.setPayloadJson(objectMapper.writeValueAsString(request.payload()));
            return value;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("审计回执 payload 无法序列化");
        }
    }

    /** 判断字符串是否为空。 */
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /** 判断字符串是否为小写或大写 SHA-256 十六进制摘要。 */
    private boolean sha256(String value) {
        return value != null && value.matches("[A-Fa-f0-9]{64}");
    }

    /** 表示内部审计回执写入请求。 */
    public record AuditReceiptRequest(
            String receiptId,
            String requestId,
            String workspaceId,
            String incidentId,
            String traceId,
            String toolName,
            String actorId,
            String approvalId,
            String requestDigest,
            String resultDigest,
            String actionStatus,
            JsonNode payload) {
    }

    /** 表示 receiptId 摘要冲突。 */
    public static final class AuditReceiptConflictException extends RuntimeException {
        /** 创建审计回执冲突异常。 */
        public AuditReceiptConflictException(String message) {
            super(message);
        }
    }
}
