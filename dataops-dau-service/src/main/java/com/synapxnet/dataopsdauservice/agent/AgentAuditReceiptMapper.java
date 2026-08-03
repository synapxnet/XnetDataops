package com.synapxnet.dataopsdauservice.agent;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 持久化追加式 Agent 审计回执镜像。
 */
@Mapper
public interface AgentAuditReceiptMapper {

    /**
     * 根据回执 UID 查询现有镜像。
     *
     * @param receiptId 回执 UID
     * @return 已有镜像，不存在时返回 null
     */
    @Select("SELECT * FROM xnet_dataops_agent_audit_receipt WHERE receipt_id = #{receiptId} LIMIT 1")
    AgentAuditReceiptMirror findByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 追加写入审计回执；不提供更新或物理删除方法。
     *
     * @param receipt 审计回执镜像
     * @return 影响行数
     */
    @Insert("INSERT INTO xnet_dataops_agent_audit_receipt "
            + "(receipt_id, request_id, workspace_id, incident_id, trace_id, tool_name, actor_id, approval_id, "
            + "request_digest, result_digest, action_status, payload_json) VALUES "
            + "(#{receiptId}, #{requestId}, #{workspaceId}, #{incidentId}, #{traceId}, #{toolName}, #{actorId}, "
            + "#{approvalId}, #{requestDigest}, #{resultDigest}, #{actionStatus}, #{payloadJson})")
    int insert(AgentAuditReceiptMirror receipt);
}
