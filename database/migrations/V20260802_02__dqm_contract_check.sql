-- Copyright (C) 2026 Synapxnet. All rights reserved.
-- This file is Synapxnet Proprietary and Confidential. It is strictly
-- forbidden to copy, distribute, or use without explicit authorization.
-- 用途：恢复原生证据结构，不含演练数据。Purpose: Restore native evidence schema without fixture data.
-- Author: maoyo | Department: 研发部 | Date: 2026-09-13
-- Version: 1.0.0 | Security Level: INTERNAL
-- __version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
-- __maintainer__: maoyo | __email__: synapxnet@gmail.com
-- GOAI 1.0.0: DQM 已知契约检查事实，不复制 MLOps 契约全文。
CREATE TABLE IF NOT EXISTS `xnet_dataops_dqm_contract_check` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid` VARCHAR(64) NOT NULL COMMENT '稳定外部 UID',
    `report_uid` VARCHAR(64) NOT NULL COMMENT '质量报告 UID',
    `asset_uid` VARCHAR(64) NOT NULL COMMENT '数据资产 UID',
    `schema_snapshot_uid` VARCHAR(64) NOT NULL COMMENT 'Schema 快照 UID',
    `contract_ref` VARCHAR(128) NOT NULL COMMENT '受控模型契约引用',
    `expected_field_count` INT NOT NULL COMMENT '期望字段数',
    `actual_field_count` INT NOT NULL COMMENT '实际字段数',
    `status` VARCHAR(16) NOT NULL COMMENT 'PASSED/FAILED/UNKNOWN',
    `mismatch_json` JSON NULL COMMENT '不含样本值的差异摘要',
    `checked_at` DATETIME(3) NOT NULL COMMENT 'UTC 检查时间',
    `incident_id` VARCHAR(64) NULL COMMENT 'OpenXnet Incident ID',
    `trace_id` VARCHAR(64) NULL COMMENT 'OpenXnet Trace ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dqm_contract_uid` (`uid`),
    KEY `idx_dqm_contract_report` (`report_uid`),
    KEY `idx_dqm_contract_trace` (`incident_id`, `trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI 数据契约检查';
