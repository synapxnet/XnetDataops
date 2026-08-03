-- GOAI 1.0.0: DGV Schema 快照，所有时间按 UTC 写入。
CREATE TABLE IF NOT EXISTS `xnet_dataops_dgv_schema_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid` VARCHAR(64) NOT NULL COMMENT '稳定外部 UID',
    `asset_uid` VARCHAR(64) NOT NULL COMMENT '数据资产 UID',
    `asset_name` VARCHAR(255) NOT NULL COMMENT '数据资产名称',
    `schema_version` VARCHAR(64) NOT NULL COMMENT '资产内 Schema 版本',
    `field_count` INT NOT NULL COMMENT '字段数',
    `schema_hash` CHAR(64) NOT NULL COMMENT '规范化字段 JSON SHA-256',
    `schema_json` JSON NOT NULL COMMENT '不含样本值的字段契约',
    `source_type` VARCHAR(32) NOT NULL COMMENT 'CATALOG/PIPELINE/MANUAL',
    `captured_at` DATETIME(3) NOT NULL COMMENT 'UTC 观测时间',
    `created_by` VARCHAR(64) NOT NULL COMMENT '捕获主体',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dgv_snapshot_uid` (`uid`),
    UNIQUE KEY `uk_dgv_asset_version` (`asset_uid`, `schema_version`),
    KEY `idx_dgv_asset_captured` (`asset_uid`, `captured_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='GOAI Schema 快照';
