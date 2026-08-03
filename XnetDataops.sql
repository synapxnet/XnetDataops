-- ============================================================
-- XnetDataops 一站式数据管理平台 - 数据库建表脚本
-- Database: XnetDataops
-- ============================================================

CREATE DATABASE IF NOT EXISTS XnetDataops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE XnetDataops;

-- ============================================================
-- USR - 用户权限模块
-- ============================================================

CREATE TABLE xnet_dataops_usr_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL DEFAULT '' COMMENT 'BCrypt加密（验证码登录可为空）',
  email VARCHAR(100),
  phone VARCHAR(20),
  user_type VARCHAR(20) DEFAULT 'user' COMMENT 'admin/user',
  status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
  last_login_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE xnet_dataops_usr_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE xnet_dataops_usr_user_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  cluster_id BIGINT DEFAULT 0 COMMENT '集群/租户ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  INDEX idx_user (user_id),
  INDEX idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE xnet_dataops_usr_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(500) NOT NULL,
  ip VARCHAR(50),
  expire_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ============================================================
-- DSM - 数据源管理模块
-- ============================================================

CREATE TABLE xnet_dataops_dsm_datasource (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(30) NOT NULL COMMENT 'MYSQL/POSTGRESQL/ORACLE/HIVE/KAFKA/S3/FTP/API',
  host VARCHAR(200),
  port INT,
  database_name VARCHAR(100),
  username VARCHAR(100),
  encrypted_password VARCHAR(500),
  connection_params TEXT COMMENT 'JSON格式额外连接参数',
  status VARCHAR(20) DEFAULT 'inactive' COMMENT 'active/inactive/error',
  last_test_at DATETIME,
  description TEXT,
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_type (type),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- ============================================================
-- DIM - 数据集成模块
-- ============================================================

CREATE TABLE xnet_dataops_dim_sync_task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  source_ds_id BIGINT NOT NULL COMMENT '源数据源ID',
  target_ds_id BIGINT NOT NULL COMMENT '目标数据源ID',
  source_table VARCHAR(200) NOT NULL,
  target_table VARCHAR(200) NOT NULL,
  sync_mode VARCHAR(20) DEFAULT 'full' COMMENT 'full/incremental',
  incremental_field VARCHAR(100) COMMENT '增量字段名',
  schedule_cron VARCHAR(50) COMMENT 'Cron表达式',
  status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/online/offline/running/error',
  description TEXT,
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步任务表';

CREATE TABLE xnet_dataops_dim_field_mapping (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  source_field VARCHAR(100) NOT NULL,
  target_field VARCHAR(100) NOT NULL,
  transform_expression VARCHAR(500) COMMENT '转换表达式',
  sort_order INT DEFAULT 0,
  FOREIGN KEY (task_id) REFERENCES xnet_dataops_dim_sync_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段映射表';

CREATE TABLE xnet_dataops_dim_sync_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  task_id BIGINT NOT NULL,
  start_time DATETIME,
  end_time DATETIME,
  status VARCHAR(20) DEFAULT 'running' COMMENT 'running/success/failed',
  rows_read BIGINT DEFAULT 0,
  rows_written BIGINT DEFAULT 0,
  error_msg TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (task_id) REFERENCES xnet_dataops_dim_sync_task(id) ON DELETE CASCADE,
  INDEX idx_task_id (task_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步执行日志表';

-- ============================================================
-- DDV - 数据开发模块
-- ============================================================

CREATE TABLE xnet_dataops_ddv_sql_script (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  datasource_id BIGINT COMMENT '关联数据源',
  content LONGTEXT COMMENT '脚本内容',
  script_type VARCHAR(20) DEFAULT 'sql' COMMENT 'sql/python/shell',
  folder_path VARCHAR(500) DEFAULT '/' COMMENT '虚拟目录路径',
  status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/published',
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL脚本表';

CREATE TABLE xnet_dataops_ddv_query_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  datasource_id BIGINT,
  sql_content LONGTEXT NOT NULL,
  execute_status VARCHAR(20) COMMENT 'success/failed',
  rows_affected BIGINT DEFAULT 0,
  duration_ms BIGINT DEFAULT 0 COMMENT '执行耗时（毫秒）',
  error_msg TEXT,
  executed_by VARCHAR(50),
  executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_datasource (datasource_id),
  INDEX idx_executed_by (executed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询历史表';

CREATE TABLE xnet_dataops_ddv_saved_query (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  datasource_id BIGINT,
  sql_content LONGTEXT NOT NULL,
  description TEXT,
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏查询表';

-- ============================================================
-- TSK - 任务调度模块
-- ============================================================

CREATE TABLE xnet_dataops_tsk_workflow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  schedule_cron VARCHAR(50) COMMENT 'Cron定时表达式',
  status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/online/offline',
  dag_json LONGTEXT COMMENT 'DAG图形布局JSON',
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

CREATE TABLE xnet_dataops_tsk_workflow_node (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  node_key VARCHAR(50) NOT NULL COMMENT '节点唯一标识',
  node_name VARCHAR(100),
  node_type VARCHAR(30) NOT NULL COMMENT 'sql/sync/shell/python/http',
  config_json TEXT COMMENT '节点配置JSON（SQL内容、脚本路径等）',
  position_x DOUBLE DEFAULT 0,
  position_y DOUBLE DEFAULT 0,
  FOREIGN KEY (workflow_id) REFERENCES xnet_dataops_tsk_workflow(id) ON DELETE CASCADE,
  UNIQUE KEY uk_workflow_node (workflow_id, node_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流DAG节点表';

CREATE TABLE xnet_dataops_tsk_workflow_edge (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  source_node_key VARCHAR(50) NOT NULL,
  target_node_key VARCHAR(50) NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES xnet_dataops_tsk_workflow(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流DAG边表';

CREATE TABLE xnet_dataops_tsk_task_instance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  workflow_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/success/failed/cancelled',
  trigger_type VARCHAR(20) DEFAULT 'manual' COMMENT 'manual/schedule',
  start_time DATETIME,
  end_time DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (workflow_id) REFERENCES xnet_dataops_tsk_workflow(id) ON DELETE CASCADE,
  INDEX idx_workflow (workflow_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流运行实例表';

CREATE TABLE xnet_dataops_tsk_node_instance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_instance_id BIGINT NOT NULL,
  node_key VARCHAR(50) NOT NULL,
  status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/success/failed/skipped',
  start_time DATETIME,
  end_time DATETIME,
  log_content LONGTEXT,
  FOREIGN KEY (task_instance_id) REFERENCES xnet_dataops_tsk_task_instance(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点运行实例表';

-- ============================================================
-- DQM - 数据质量模块
-- ============================================================

CREATE TABLE xnet_dataops_dqm_quality_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  datasource_id BIGINT NOT NULL,
  table_name VARCHAR(200) NOT NULL,
  column_name VARCHAR(100) COMMENT '为空则为表级规则',
  rule_type VARCHAR(30) NOT NULL COMMENT 'not_null/unique/range/regex/custom_sql',
  rule_expression TEXT COMMENT '规则表达式或自定义SQL',
  severity VARCHAR(20) DEFAULT 'warning' COMMENT 'info/warning/critical',
  enabled BOOLEAN DEFAULT TRUE,
  schedule_cron VARCHAR(50) COMMENT '定时检测Cron',
  description TEXT,
  created_by VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_datasource (datasource_id),
  INDEX idx_rule_type (rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量规则表';

CREATE TABLE xnet_dataops_dqm_quality_report (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  rule_id BIGINT NOT NULL,
  check_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL COMMENT 'passed/failed',
  total_rows BIGINT DEFAULT 0,
  failed_rows BIGINT DEFAULT 0,
  pass_rate DECIMAL(5,2) DEFAULT 0 COMMENT '通过率百分比',
  detail_json LONGTEXT COMMENT '详细检测结果JSON',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (rule_id) REFERENCES xnet_dataops_dqm_quality_rule(id) ON DELETE CASCADE,
  INDEX idx_rule (rule_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检测报告表';

CREATE TABLE xnet_dataops_dqm_quality_alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  rule_id BIGINT NOT NULL,
  report_id BIGINT,
  alert_level VARCHAR(20) NOT NULL COMMENT 'info/warning/critical',
  message TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'open' COMMENT 'open/acknowledged/resolved',
  triggered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  resolved_at DATETIME,
  FOREIGN KEY (rule_id) REFERENCES xnet_dataops_dqm_quality_rule(id) ON DELETE CASCADE,
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量告警表';

-- ============================================================
-- DGV - 数据治理模块
-- ============================================================

CREATE TABLE xnet_dataops_dgv_meta_table (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  datasource_id BIGINT NOT NULL,
  schema_name VARCHAR(100),
  table_name VARCHAR(200) NOT NULL,
  table_type VARCHAR(20) DEFAULT 'table' COMMENT 'table/view',
  row_count BIGINT DEFAULT 0,
  data_size_bytes BIGINT DEFAULT 0,
  description TEXT,
  owner VARCHAR(50),
  last_sync_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_datasource (datasource_id),
  UNIQUE KEY uk_ds_schema_table (datasource_id, schema_name, table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据表';

CREATE TABLE xnet_dataops_dgv_meta_column (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  meta_table_id BIGINT NOT NULL,
  column_name VARCHAR(100) NOT NULL,
  column_type VARCHAR(100),
  is_nullable BOOLEAN DEFAULT TRUE,
  is_primary_key BOOLEAN DEFAULT FALSE,
  default_value VARCHAR(500),
  description TEXT,
  sort_order INT DEFAULT 0,
  FOREIGN KEY (meta_table_id) REFERENCES xnet_dataops_dgv_meta_table(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据列表';

CREATE TABLE xnet_dataops_dgv_data_lineage (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  uid VARCHAR(36) NOT NULL UNIQUE,
  source_table_id BIGINT NOT NULL,
  target_table_id BIGINT NOT NULL,
  transform_type VARCHAR(20) COMMENT 'etl/sql/api',
  relationship_desc TEXT,
  workflow_id BIGINT COMMENT '关联的工作流ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (source_table_id) REFERENCES xnet_dataops_dgv_meta_table(id) ON DELETE CASCADE,
  FOREIGN KEY (target_table_id) REFERENCES xnet_dataops_dgv_meta_table(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘关系表';

CREATE TABLE xnet_dataops_dgv_data_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  tag_type VARCHAR(30) NOT NULL COMMENT 'classification/sensitivity/business',
  color VARCHAR(20) DEFAULT '#1890ff',
  description VARCHAR(200),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据标签表';

CREATE TABLE xnet_dataops_dgv_table_tag (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  meta_table_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (meta_table_id) REFERENCES xnet_dataops_dgv_meta_table(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES xnet_dataops_dgv_data_tag(id) ON DELETE CASCADE,
  UNIQUE KEY uk_table_tag (meta_table_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表-标签关联表';

-- ============================================================
-- 初始数据
-- ============================================================

INSERT INTO xnet_dataops_usr_user (uid, username, password, phone, user_type, status)
VALUES (UUID(), 'admin', '', '17870171303', 'admin', 'active');

INSERT INTO xnet_dataops_usr_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('数据开发', 'DEVELOPER', '数据开发人员，可管理数据源和开发任务'),
('数据分析师', 'ANALYST', '数据分析师，可查询和查看数据'),
('观察者', 'VIEWER', '只读权限，查看平台状态');

-- 获取超级管理员用户ID和ADMIN角色ID，插入关联表
SET @admin_user_id = (SELECT id FROM xnet_dataops_usr_user WHERE phone = '17870171303' LIMIT 1);
SET @admin_role_id = (SELECT id FROM xnet_dataops_usr_role WHERE role_code = 'ADMIN' LIMIT 1);
INSERT INTO xnet_dataops_usr_user_role (user_id, role_id) VALUES (@admin_user_id, @admin_role_id);

INSERT INTO xnet_dataops_dgv_data_tag (name, tag_type, color, description) VALUES
('个人信息', 'sensitivity', '#ff4d4f', '包含个人隐私数据'),
('核心业务', 'business', '#1890ff', '核心业务数据'),
('公开数据', 'classification', '#52c41a', '可公开访问的数据'),
('机密数据', 'classification', '#ff4d4f', '机密数据，需授权访问'),
('内部数据', 'classification', '#faad14', '内部使用数据');


-- ============================================================
-- GOAI Competition 1.0.0
-- Canonical source: database/migrations
-- ============================================================

-- V20260802_01__dgv_schema_snapshot.sql
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

-- V20260802_02__dqm_contract_check.sql
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

-- V20260802_03__dau_agent_audit_receipt.sql
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

-- V20260802_04__dob_incident_link.sql
-- GOAI 1.0.0: 为 DOB 事件补充跨平台证据关联；脚本可重复执行。
SET @schema_name = DATABASE();
SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event') = 1
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event'
       AND COLUMN_NAME = 'incident_id') = 0,
    'ALTER TABLE xnet_dataops_dob_monitor_event ADD COLUMN incident_id VARCHAR(64) NULL COMMENT ''OpenXnet Incident ID''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt;
EXECUTE goai_stmt;
DEALLOCATE PREPARE goai_stmt;

SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event') = 1
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event'
       AND COLUMN_NAME = 'trace_id') = 0,
    'ALTER TABLE xnet_dataops_dob_monitor_event ADD COLUMN trace_id VARCHAR(64) NULL COMMENT ''OpenXnet Trace ID''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt;
EXECUTE goai_stmt;
DEALLOCATE PREPARE goai_stmt;

SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event') = 1
    AND (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event'
       AND COLUMN_NAME = 'evidence_id') = 0,
    'ALTER TABLE xnet_dataops_dob_monitor_event ADD COLUMN evidence_id VARCHAR(64) NULL COMMENT ''Agent Evidence ID''',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt;
EXECUTE goai_stmt;
DEALLOCATE PREPARE goai_stmt;

SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event') = 1
    AND (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'xnet_dataops_dob_monitor_event'
       AND INDEX_NAME = 'idx_dob_incident_trace') = 0,
    'ALTER TABLE xnet_dataops_dob_monitor_event ADD INDEX idx_dob_incident_trace (incident_id, trace_id)',
    'SELECT 1');
PREPARE goai_stmt FROM @stmt;
EXECUTE goai_stmt;
DEALLOCATE PREPARE goai_stmt;

-- V20260802_05__goai_fixture.sql
-- GOAI 1.0.0 固定演示 Fixture。数据进入真实业务表，Service 不硬编码指标。
SET time_zone = '+00:00';

INSERT INTO xnet_dataops_dgv_meta_table
(uid, datasource_id, schema_name, table_name, table_type, row_count, data_size_bytes, description, owner, last_sync_at)
VALUES
('asset_raw_transactions', 9001, 'prod', 'raw_transactions', 'table', 1250000, 536870912, '脱敏交易事实源', 'risk-data-team', '2026-08-02 09:55:00'),
('asset_risk_features_prod', 9001, 'prod', 'risk_features', 'table', 1200000, 429916160, '生产风险特征资产，当前输出 120 维', 'risk-data-team', '2026-08-02 10:00:00'),
('asset_risk_predictions', 9001, 'prod', 'risk_predictions', 'table', 1180000, 214958080, '风险模型推理结果', 'risk-ml-team', '2026-08-02 10:05:00')
ON DUPLICATE KEY UPDATE
description = VALUES(description), row_count = VALUES(row_count), last_sync_at = VALUES(last_sync_at);

INSERT INTO xnet_dataops_dgv_schema_snapshot
(uid, asset_uid, asset_name, schema_version, field_count, schema_hash, schema_json, source_type, captured_at, created_by)
VALUES
('schema_risk_features_120', 'asset_risk_features_prod', 'prod.risk_features', '120', 120,
 'db316857073f557d89c4b3fb03e082c53a33e2d50f616d27b86ef4bac6b5dbd7', CAST('[{"name":"customer_id","type":"STRING","nullable":false,"ordinal":1,"description":"业务主键/事件时间"},{"name":"event_time","type":"TIMESTAMP","nullable":false,"ordinal":2,"description":"业务主键/事件时间"},{"name":"risk_feature_003","type":"DOUBLE","nullable":true,"ordinal":3,"description":"脱敏风险特征"},{"name":"risk_feature_004","type":"DOUBLE","nullable":true,"ordinal":4,"description":"脱敏风险特征"},{"name":"risk_feature_005","type":"DOUBLE","nullable":true,"ordinal":5,"description":"脱敏风险特征"},{"name":"risk_feature_006","type":"DOUBLE","nullable":true,"ordinal":6,"description":"脱敏风险特征"},{"name":"risk_feature_007","type":"DOUBLE","nullable":true,"ordinal":7,"description":"脱敏风险特征"},{"name":"risk_feature_008","type":"DOUBLE","nullable":true,"ordinal":8,"description":"脱敏风险特征"},{"name":"risk_feature_009","type":"DOUBLE","nullable":true,"ordinal":9,"description":"脱敏风险特征"},{"name":"risk_feature_010","type":"DOUBLE","nullable":true,"ordinal":10,"description":"脱敏风险特征"},{"name":"risk_feature_011","type":"DOUBLE","nullable":true,"ordinal":11,"description":"脱敏风险特征"},{"name":"risk_feature_012","type":"DOUBLE","nullable":true,"ordinal":12,"description":"脱敏风险特征"},{"name":"risk_feature_013","type":"DOUBLE","nullable":true,"ordinal":13,"description":"脱敏风险特征"},{"name":"risk_feature_014","type":"DOUBLE","nullable":true,"ordinal":14,"description":"脱敏风险特征"},{"name":"risk_feature_015","type":"DOUBLE","nullable":true,"ordinal":15,"description":"脱敏风险特征"},{"name":"risk_feature_016","type":"DOUBLE","nullable":true,"ordinal":16,"description":"脱敏风险特征"},{"name":"risk_feature_017","type":"DOUBLE","nullable":true,"ordinal":17,"description":"脱敏风险特征"},{"name":"risk_feature_018","type":"DOUBLE","nullable":true,"ordinal":18,"description":"脱敏风险特征"},{"name":"risk_feature_019","type":"DOUBLE","nullable":true,"ordinal":19,"description":"脱敏风险特征"},{"name":"risk_feature_020","type":"DOUBLE","nullable":true,"ordinal":20,"description":"脱敏风险特征"},{"name":"risk_feature_021","type":"DOUBLE","nullable":true,"ordinal":21,"description":"脱敏风险特征"},{"name":"risk_feature_022","type":"DOUBLE","nullable":true,"ordinal":22,"description":"脱敏风险特征"},{"name":"risk_feature_023","type":"DOUBLE","nullable":true,"ordinal":23,"description":"脱敏风险特征"},{"name":"risk_feature_024","type":"DOUBLE","nullable":true,"ordinal":24,"description":"脱敏风险特征"},{"name":"risk_feature_025","type":"DOUBLE","nullable":true,"ordinal":25,"description":"脱敏风险特征"},{"name":"risk_feature_026","type":"DOUBLE","nullable":true,"ordinal":26,"description":"脱敏风险特征"},{"name":"risk_feature_027","type":"DOUBLE","nullable":true,"ordinal":27,"description":"脱敏风险特征"},{"name":"risk_feature_028","type":"DOUBLE","nullable":true,"ordinal":28,"description":"脱敏风险特征"},{"name":"risk_feature_029","type":"DOUBLE","nullable":true,"ordinal":29,"description":"脱敏风险特征"},{"name":"risk_feature_030","type":"DOUBLE","nullable":true,"ordinal":30,"description":"脱敏风险特征"},{"name":"risk_feature_031","type":"DOUBLE","nullable":true,"ordinal":31,"description":"脱敏风险特征"},{"name":"risk_feature_032","type":"DOUBLE","nullable":true,"ordinal":32,"description":"脱敏风险特征"},{"name":"risk_feature_033","type":"DOUBLE","nullable":true,"ordinal":33,"description":"脱敏风险特征"},{"name":"risk_feature_034","type":"DOUBLE","nullable":true,"ordinal":34,"description":"脱敏风险特征"},{"name":"risk_feature_035","type":"DOUBLE","nullable":true,"ordinal":35,"description":"脱敏风险特征"},{"name":"risk_feature_036","type":"DOUBLE","nullable":true,"ordinal":36,"description":"脱敏风险特征"},{"name":"risk_feature_037","type":"DOUBLE","nullable":true,"ordinal":37,"description":"脱敏风险特征"},{"name":"risk_feature_038","type":"DOUBLE","nullable":true,"ordinal":38,"description":"脱敏风险特征"},{"name":"risk_feature_039","type":"DOUBLE","nullable":true,"ordinal":39,"description":"脱敏风险特征"},{"name":"risk_feature_040","type":"DOUBLE","nullable":true,"ordinal":40,"description":"脱敏风险特征"},{"name":"risk_feature_041","type":"DOUBLE","nullable":true,"ordinal":41,"description":"脱敏风险特征"},{"name":"risk_feature_042","type":"DOUBLE","nullable":true,"ordinal":42,"description":"脱敏风险特征"},{"name":"risk_feature_043","type":"DOUBLE","nullable":true,"ordinal":43,"description":"脱敏风险特征"},{"name":"risk_feature_044","type":"DOUBLE","nullable":true,"ordinal":44,"description":"脱敏风险特征"},{"name":"risk_feature_045","type":"DOUBLE","nullable":true,"ordinal":45,"description":"脱敏风险特征"},{"name":"risk_feature_046","type":"DOUBLE","nullable":true,"ordinal":46,"description":"脱敏风险特征"},{"name":"risk_feature_047","type":"DOUBLE","nullable":true,"ordinal":47,"description":"脱敏风险特征"},{"name":"risk_feature_048","type":"DOUBLE","nullable":true,"ordinal":48,"description":"脱敏风险特征"},{"name":"risk_feature_049","type":"DOUBLE","nullable":true,"ordinal":49,"description":"脱敏风险特征"},{"name":"risk_feature_050","type":"DOUBLE","nullable":true,"ordinal":50,"description":"脱敏风险特征"},{"name":"risk_feature_051","type":"DOUBLE","nullable":true,"ordinal":51,"description":"脱敏风险特征"},{"name":"risk_feature_052","type":"DOUBLE","nullable":true,"ordinal":52,"description":"脱敏风险特征"},{"name":"risk_feature_053","type":"DOUBLE","nullable":true,"ordinal":53,"description":"脱敏风险特征"},{"name":"risk_feature_054","type":"DOUBLE","nullable":true,"ordinal":54,"description":"脱敏风险特征"},{"name":"risk_feature_055","type":"DOUBLE","nullable":true,"ordinal":55,"description":"脱敏风险特征"},{"name":"risk_feature_056","type":"DOUBLE","nullable":true,"ordinal":56,"description":"脱敏风险特征"},{"name":"risk_feature_057","type":"DOUBLE","nullable":true,"ordinal":57,"description":"脱敏风险特征"},{"name":"risk_feature_058","type":"DOUBLE","nullable":true,"ordinal":58,"description":"脱敏风险特征"},{"name":"risk_feature_059","type":"DOUBLE","nullable":true,"ordinal":59,"description":"脱敏风险特征"},{"name":"risk_feature_060","type":"DOUBLE","nullable":true,"ordinal":60,"description":"脱敏风险特征"},{"name":"risk_feature_061","type":"DOUBLE","nullable":true,"ordinal":61,"description":"脱敏风险特征"},{"name":"risk_feature_062","type":"DOUBLE","nullable":true,"ordinal":62,"description":"脱敏风险特征"},{"name":"risk_feature_063","type":"DOUBLE","nullable":true,"ordinal":63,"description":"脱敏风险特征"},{"name":"risk_feature_064","type":"DOUBLE","nullable":true,"ordinal":64,"description":"脱敏风险特征"},{"name":"risk_feature_065","type":"DOUBLE","nullable":true,"ordinal":65,"description":"脱敏风险特征"},{"name":"risk_feature_066","type":"DOUBLE","nullable":true,"ordinal":66,"description":"脱敏风险特征"},{"name":"risk_feature_067","type":"DOUBLE","nullable":true,"ordinal":67,"description":"脱敏风险特征"},{"name":"risk_feature_068","type":"DOUBLE","nullable":true,"ordinal":68,"description":"脱敏风险特征"},{"name":"risk_feature_069","type":"DOUBLE","nullable":true,"ordinal":69,"description":"脱敏风险特征"},{"name":"risk_feature_070","type":"DOUBLE","nullable":true,"ordinal":70,"description":"脱敏风险特征"},{"name":"risk_feature_071","type":"DOUBLE","nullable":true,"ordinal":71,"description":"脱敏风险特征"},{"name":"risk_feature_072","type":"DOUBLE","nullable":true,"ordinal":72,"description":"脱敏风险特征"},{"name":"risk_feature_073","type":"DOUBLE","nullable":true,"ordinal":73,"description":"脱敏风险特征"},{"name":"risk_feature_074","type":"DOUBLE","nullable":true,"ordinal":74,"description":"脱敏风险特征"},{"name":"risk_feature_075","type":"DOUBLE","nullable":true,"ordinal":75,"description":"脱敏风险特征"},{"name":"risk_feature_076","type":"DOUBLE","nullable":true,"ordinal":76,"description":"脱敏风险特征"},{"name":"risk_feature_077","type":"DOUBLE","nullable":true,"ordinal":77,"description":"脱敏风险特征"},{"name":"risk_feature_078","type":"DOUBLE","nullable":true,"ordinal":78,"description":"脱敏风险特征"},{"name":"risk_feature_079","type":"DOUBLE","nullable":true,"ordinal":79,"description":"脱敏风险特征"},{"name":"risk_feature_080","type":"DOUBLE","nullable":true,"ordinal":80,"description":"脱敏风险特征"},{"name":"risk_feature_081","type":"DOUBLE","nullable":true,"ordinal":81,"description":"脱敏风险特征"},{"name":"risk_feature_082","type":"DOUBLE","nullable":true,"ordinal":82,"description":"脱敏风险特征"},{"name":"risk_feature_083","type":"DOUBLE","nullable":true,"ordinal":83,"description":"脱敏风险特征"},{"name":"risk_feature_084","type":"DOUBLE","nullable":true,"ordinal":84,"description":"脱敏风险特征"},{"name":"risk_feature_085","type":"DOUBLE","nullable":true,"ordinal":85,"description":"脱敏风险特征"},{"name":"risk_feature_086","type":"DOUBLE","nullable":true,"ordinal":86,"description":"脱敏风险特征"},{"name":"risk_feature_087","type":"DOUBLE","nullable":true,"ordinal":87,"description":"脱敏风险特征"},{"name":"risk_feature_088","type":"DOUBLE","nullable":true,"ordinal":88,"description":"脱敏风险特征"},{"name":"risk_feature_089","type":"DOUBLE","nullable":true,"ordinal":89,"description":"脱敏风险特征"},{"name":"risk_feature_090","type":"DOUBLE","nullable":true,"ordinal":90,"description":"脱敏风险特征"},{"name":"risk_feature_091","type":"DOUBLE","nullable":true,"ordinal":91,"description":"脱敏风险特征"},{"name":"risk_feature_092","type":"DOUBLE","nullable":true,"ordinal":92,"description":"脱敏风险特征"},{"name":"risk_feature_093","type":"DOUBLE","nullable":true,"ordinal":93,"description":"脱敏风险特征"},{"name":"risk_feature_094","type":"DOUBLE","nullable":true,"ordinal":94,"description":"脱敏风险特征"},{"name":"risk_feature_095","type":"DOUBLE","nullable":true,"ordinal":95,"description":"脱敏风险特征"},{"name":"risk_feature_096","type":"DOUBLE","nullable":true,"ordinal":96,"description":"脱敏风险特征"},{"name":"risk_feature_097","type":"DOUBLE","nullable":true,"ordinal":97,"description":"脱敏风险特征"},{"name":"risk_feature_098","type":"DOUBLE","nullable":true,"ordinal":98,"description":"脱敏风险特征"},{"name":"risk_feature_099","type":"DOUBLE","nullable":true,"ordinal":99,"description":"脱敏风险特征"},{"name":"risk_feature_100","type":"DOUBLE","nullable":true,"ordinal":100,"description":"脱敏风险特征"},{"name":"risk_feature_101","type":"DOUBLE","nullable":true,"ordinal":101,"description":"脱敏风险特征"},{"name":"risk_feature_102","type":"DOUBLE","nullable":true,"ordinal":102,"description":"脱敏风险特征"},{"name":"risk_feature_103","type":"DOUBLE","nullable":true,"ordinal":103,"description":"脱敏风险特征"},{"name":"risk_feature_104","type":"DOUBLE","nullable":true,"ordinal":104,"description":"脱敏风险特征"},{"name":"risk_feature_105","type":"DOUBLE","nullable":true,"ordinal":105,"description":"脱敏风险特征"},{"name":"risk_feature_106","type":"DOUBLE","nullable":true,"ordinal":106,"description":"脱敏风险特征"},{"name":"risk_feature_107","type":"DOUBLE","nullable":true,"ordinal":107,"description":"脱敏风险特征"},{"name":"risk_feature_108","type":"DOUBLE","nullable":true,"ordinal":108,"description":"脱敏风险特征"},{"name":"risk_feature_109","type":"DOUBLE","nullable":true,"ordinal":109,"description":"脱敏风险特征"},{"name":"risk_feature_110","type":"DOUBLE","nullable":true,"ordinal":110,"description":"脱敏风险特征"},{"name":"risk_feature_111","type":"DOUBLE","nullable":true,"ordinal":111,"description":"脱敏风险特征"},{"name":"risk_feature_112","type":"DOUBLE","nullable":true,"ordinal":112,"description":"脱敏风险特征"},{"name":"risk_feature_113","type":"DOUBLE","nullable":true,"ordinal":113,"description":"脱敏风险特征"},{"name":"risk_feature_114","type":"DOUBLE","nullable":true,"ordinal":114,"description":"脱敏风险特征"},{"name":"risk_feature_115","type":"DOUBLE","nullable":true,"ordinal":115,"description":"脱敏风险特征"},{"name":"risk_feature_116","type":"DOUBLE","nullable":true,"ordinal":116,"description":"脱敏风险特征"},{"name":"risk_feature_117","type":"DOUBLE","nullable":true,"ordinal":117,"description":"脱敏风险特征"},{"name":"risk_feature_118","type":"DOUBLE","nullable":true,"ordinal":118,"description":"脱敏风险特征"},{"name":"risk_feature_119","type":"DOUBLE","nullable":true,"ordinal":119,"description":"脱敏风险特征"},{"name":"risk_feature_120","type":"DOUBLE","nullable":true,"ordinal":120,"description":"脱敏风险特征"}]' AS JSON), 'PIPELINE', '2026-08-02 10:00:00.000', 'goai-fixture')
ON DUPLICATE KEY UPDATE
field_count = VALUES(field_count), schema_hash = VALUES(schema_hash), schema_json = VALUES(schema_json),
source_type = VALUES(source_type), captured_at = VALUES(captured_at);

INSERT INTO xnet_dataops_tsk_workflow
(uid, name, description, schedule_cron, status, dag_json, created_by)
VALUES
('workflow_risk_features', '风险特征生产链路', '从脱敏交易事实生成 120 维风险特征', '0 */10 * * * ?', 'online',
 '{"nodes":[{"key":"extract"},{"key":"feature-build"},{"key":"publish"}]}', 'goai-fixture')
ON DUPLICATE KEY UPDATE description = VALUES(description), status = VALUES(status), dag_json = VALUES(dag_json);

SET @workflow_id = (SELECT id FROM xnet_dataops_tsk_workflow WHERE uid = 'workflow_risk_features');
INSERT INTO xnet_dataops_tsk_workflow_node
(workflow_id, node_key, node_name, node_type, config_json, position_x, position_y)
VALUES
(@workflow_id, 'extract', '读取脱敏交易事实', 'sql', '{"assetRef":"asset_raw_transactions"}', 80, 120),
(@workflow_id, 'feature-build', '构建 120 维风险特征', 'python', '{"contractRef":"contract_risk_v17","outputDimension":120}', 320, 120),
(@workflow_id, 'publish', '发布生产特征', 'sync',
 '{"assetRef":"asset_risk_features_prod","outputAssetUid":"asset_risk_features_prod","schemaSnapshotUid":"schema_risk_features_120"}', 560, 120)
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), node_type = VALUES(node_type), config_json = VALUES(config_json);

INSERT INTO xnet_dataops_tsk_task_instance
(uid, workflow_id, status, trigger_type, start_time, end_time, created_at)
VALUES
('task_risk_features_latest', @workflow_id, 'success', 'schedule', '2026-08-02 09:58:00', '2026-08-02 10:00:00', '2026-08-02 09:58:00')
ON DUPLICATE KEY UPDATE status = VALUES(status), start_time = VALUES(start_time), end_time = VALUES(end_time);

SET @task_id = (SELECT id FROM xnet_dataops_tsk_task_instance WHERE uid = 'task_risk_features_latest');
DELETE FROM xnet_dataops_tsk_node_instance WHERE task_instance_id = @task_id;
INSERT INTO xnet_dataops_tsk_node_instance
(task_instance_id, node_key, status, start_time, end_time, log_content)
VALUES
(@task_id, 'extract', 'success', '2026-08-02 09:58:00', '2026-08-02 09:58:35', '读取 1250000 行脱敏交易事实，未保存样本。'),
(@task_id, 'feature-build', 'success', '2026-08-02 09:58:35', '2026-08-02 09:59:42', '按 contract_risk_v17 生成 120 维特征。'),
(@task_id, 'publish', 'success', '2026-08-02 09:59:42', '2026-08-02 10:00:00', '发布 asset_risk_features_prod，schema=schema_risk_features_120。');

SET @raw_id = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'asset_raw_transactions');
SET @feature_id = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'asset_risk_features_prod');
SET @prediction_id = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'asset_risk_predictions');
INSERT INTO xnet_dataops_dgv_data_lineage
(uid, source_table_id, target_table_id, transform_type, relationship_desc, workflow_id)
VALUES
('lineage_raw_to_features', @raw_id, @feature_id, 'etl', '脱敏交易事实转换为风险特征', @workflow_id),
('lineage_features_to_pred', @feature_id, @prediction_id, 'api', '风险特征用于模型推理', @workflow_id)
ON DUPLICATE KEY UPDATE transform_type = VALUES(transform_type), workflow_id = VALUES(workflow_id);

INSERT INTO xnet_dataops_dqm_quality_rule
(uid, name, datasource_id, table_name, column_name, rule_type, rule_expression, severity, enabled, description, created_by)
VALUES
('rule_risk_schema_contract', '风险特征维度契约检查', 9001, 'prod.risk_features', NULL, 'custom_sql',
 'schema_field_count = expected_contract_dimension', 'critical', TRUE, '检查生产特征维度与当前模型契约是否一致', 'goai-fixture')
ON DUPLICATE KEY UPDATE rule_expression = VALUES(rule_expression), severity = VALUES(severity), enabled = VALUES(enabled);

SET @rule_id = (SELECT id FROM xnet_dataops_dqm_quality_rule WHERE uid = 'rule_risk_schema_contract');
INSERT INTO xnet_dataops_dqm_quality_report
(uid, rule_id, check_time, status, total_rows, failed_rows, pass_rate, detail_json)
VALUES
('qr_risk_features_120', @rule_id, '2026-08-02 10:00:00', 'failed', 1200000, 216000, 82.00,
 '{"assetUid":"asset_risk_features_prod","schemaSnapshotUid":"schema_risk_features_120","rules":[{"ruleUid":"rule_risk_schema_contract","name":"模型输入维度","status":"FAILED","failedRows":216000}],"warnings":[]}')
ON DUPLICATE KEY UPDATE status = VALUES(status), total_rows = VALUES(total_rows),
failed_rows = VALUES(failed_rows), pass_rate = VALUES(pass_rate), detail_json = VALUES(detail_json);

SET @report_id = (SELECT id FROM xnet_dataops_dqm_quality_report WHERE uid = 'qr_risk_features_120');
INSERT INTO xnet_dataops_dqm_quality_alert
(uid, rule_id, report_id, alert_level, message, status, triggered_at)
VALUES
('quality_alert_risk_contract', @rule_id, @report_id, 'critical', '生产特征为 120 维，与 risk-model-v18 的 128 维输入契约不一致', 'open', '2026-08-02 10:00:00')
ON DUPLICATE KEY UPDATE message = VALUES(message), status = VALUES(status);

INSERT INTO xnet_dataops_dqm_contract_check
(uid, report_uid, asset_uid, schema_snapshot_uid, contract_ref, expected_field_count,
 actual_field_count, status, mismatch_json, checked_at, incident_id, trace_id)
VALUES
('contract_check_risk_120_128', 'qr_risk_features_120', 'asset_risk_features_prod',
 'schema_risk_features_120', 'contract_risk_v18', 128, 120, 'FAILED',
 '{"missingDimensionCount":8,"sampleValuesStored":false}', '2026-08-02 10:00:00.000',
 'inc_model_contract_001', 'trace_model_contract_001')
ON DUPLICATE KEY UPDATE expected_field_count = VALUES(expected_field_count),
actual_field_count = VALUES(actual_field_count), status = VALUES(status),
incident_id = VALUES(incident_id), trace_id = VALUES(trace_id);

SELECT
 (SELECT field_count FROM xnet_dataops_dgv_schema_snapshot WHERE uid = 'schema_risk_features_120') = 120 AS schema_fixture_ready,
 (SELECT actual_field_count FROM xnet_dataops_dqm_contract_check WHERE uid = 'contract_check_risk_120_128') = 120 AS quality_fixture_ready,
 (SELECT COUNT(*) FROM xnet_dataops_tsk_task_instance WHERE uid = 'task_risk_features_latest') = 1 AS workflow_fixture_ready;
