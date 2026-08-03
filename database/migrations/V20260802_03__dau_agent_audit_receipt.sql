-- GOAI 1.0.0: DataOps 本地追加式审计镜像，跨平台总审计仍由 OpenXnet 管理。
CREATE TABLE IF NOT EXISTS `xnet_dataops_agent_audit_receipt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receipt_id` VARCHAR(64) NOT NULL COMMENT '公共回执 UID',
    `request_id` VARCHAR(128) NOT NULL COMMENT '全局请求 ID',
    `workspace_id` VARCHAR(64) NOT NULL COMMENT '企业空间 ID',
    `incident_id` VARCHAR(64) NOT NULL COMMENT '事件 ID',
    `trace_id` VARCHAR(64) NOT NULL COMMENT 'Trace ID',
    `tool_name` VARCHAR(128) NOT NULL COMMENT '工具名',
    `actor_id` VARCHAR(128) NOT NULL COMMENT '调用主体',
    `approval_id` VARCHAR(64) NULL COMMENT '审批引用',
    `request_digest` CHAR(64) NOT NULL COMMENT '请求 SHA-256',
    `result_digest` CHAR(64) NOT NULL COMMENT '结果 SHA-256',
    `action_status` VARCHAR(32) NOT NULL COMMENT '动作状态',
    `payload_json` JSON NOT NULL COMMENT '已脱敏公共回执',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'UTC 创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dataops_receipt_id` (`receipt_id`),
    KEY `idx_dataops_receipt_trace` (`workspace_id`, `incident_id`, `trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI Agent 审计回执镜像';
