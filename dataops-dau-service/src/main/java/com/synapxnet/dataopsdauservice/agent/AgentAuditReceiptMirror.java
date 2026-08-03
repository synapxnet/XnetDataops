package com.synapxnet.dataopsdauservice.agent;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表示 DataOps 本地追加式 Agent 审计回执镜像。
 */
@Data
public class AgentAuditReceiptMirror {
    private Long id;
    private String receiptId;
    private String requestId;
    private String workspaceId;
    private String incidentId;
    private String traceId;
    private String toolName;
    private String actorId;
    private String approvalId;
    private String requestDigest;
    private String resultDigest;
    private String actionStatus;
    private String payloadJson;
    private LocalDateTime createdAt;
}
