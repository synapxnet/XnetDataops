-- ============================================================
-- XnetDataops 一站式数据管理平台 - 完整数据库DDL
-- Database: XnetDataops (MySQL 8.x)
-- ============================================================

CREATE DATABASE IF NOT EXISTS XnetDataops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE XnetDataops;

-- ============================================================
-- USR - 用户管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_usr_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    user_type VARCHAR(20) DEFAULT 'normal' COMMENT 'admin/normal/readonly',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/disabled',
    last_login_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE IF NOT EXISTS xnet_dataops_usr_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE IF NOT EXISTS xnet_dataops_usr_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    cluster_id BIGINT DEFAULT 0 COMMENT '集群/租户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS xnet_dataops_usr_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    ip VARCHAR(50),
    expire_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_token (token(255))
) ENGINE=InnoDB COMMENT='用户会话表';

-- ============================================================
-- DSM - 数据源管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dsm_datasource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL COMMENT 'MYSQL/POSTGRESQL/ORACLE/HIVE/KAFKA/S3/FTP/API',
    host VARCHAR(255) NOT NULL,
    port INT,
    database_name VARCHAR(200),
    username VARCHAR(200),
    encrypted_password VARCHAR(500),
    connection_params TEXT COMMENT 'JSON格式额外参数',
    status VARCHAR(20) DEFAULT 'inactive' COMMENT 'active/inactive/error',
    last_test_at DATETIME,
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='数据源表';

-- ============================================================
-- DIM - 数据集成模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dim_sync_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    source_ds_id BIGINT NOT NULL COMMENT '源数据源ID',
    target_ds_id BIGINT NOT NULL COMMENT '目标数据源ID',
    source_table VARCHAR(200) NOT NULL,
    target_table VARCHAR(200) NOT NULL,
    sync_mode VARCHAR(20) DEFAULT 'full' COMMENT 'full/incremental',
    incremental_field VARCHAR(100),
    schedule_cron VARCHAR(100),
    status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/running/stopped/error',
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_source_ds (source_ds_id),
    INDEX idx_target_ds (target_ds_id)
) ENGINE=InnoDB COMMENT='同步任务表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dim_field_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    source_field VARCHAR(200) NOT NULL,
    target_field VARCHAR(200) NOT NULL,
    transform_expression TEXT,
    sort_order INT DEFAULT 0,
    INDEX idx_task (task_id)
) ENGINE=InnoDB COMMENT='字段映射表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dim_sync_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    task_id BIGINT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(20) DEFAULT 'running' COMMENT 'running/success/failed',
    rows_read BIGINT DEFAULT 0,
    rows_written BIGINT DEFAULT 0,
    error_msg TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='同步日志表';

-- ============================================================
-- DDV - 数据开发模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_ddv_sql_script (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    content LONGTEXT,
    script_type VARCHAR(20) DEFAULT 'sql' COMMENT 'sql/python/shell',
    folder_path VARCHAR(500) DEFAULT '/',
    status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/published',
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (script_type),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='SQL脚本表';

CREATE TABLE IF NOT EXISTS xnet_dataops_ddv_query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    datasource_id BIGINT,
    sql_content TEXT NOT NULL,
    execute_status VARCHAR(20) COMMENT 'success/failed',
    rows_affected BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    error_msg TEXT,
    executed_by VARCHAR(100),
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_executed_at (executed_at)
) ENGINE=InnoDB COMMENT='查询历史表';

CREATE TABLE IF NOT EXISTS xnet_dataops_ddv_saved_query (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    sql_content TEXT NOT NULL,
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='收藏查询表';

-- ============================================================
-- TSK - 任务调度模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_tsk_workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    schedule_cron VARCHAR(100),
    status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/online/offline',
    dag_json LONGTEXT COMMENT 'DAG结构JSON',
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='工作流表';

CREATE TABLE IF NOT EXISTS xnet_dataops_tsk_workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    node_name VARCHAR(200),
    node_type VARCHAR(50) COMMENT 'sql/sync/shell/python/http',
    config_json TEXT,
    position_x DOUBLE,
    position_y DOUBLE,
    INDEX idx_workflow (workflow_id)
) ENGINE=InnoDB COMMENT='工作流节点表';

CREATE TABLE IF NOT EXISTS xnet_dataops_tsk_workflow_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    source_node_key VARCHAR(100) NOT NULL,
    target_node_key VARCHAR(100) NOT NULL,
    INDEX idx_workflow (workflow_id)
) ENGINE=InnoDB COMMENT='工作流边表';

CREATE TABLE IF NOT EXISTS xnet_dataops_tsk_task_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    workflow_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/success/failed/cancelled',
    trigger_type VARCHAR(20) DEFAULT 'manual' COMMENT 'manual/scheduled/api',
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='任务实例表';

CREATE TABLE IF NOT EXISTS xnet_dataops_tsk_node_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_instance_id BIGINT NOT NULL,
    node_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    start_time DATETIME,
    end_time DATETIME,
    log_content LONGTEXT,
    INDEX idx_task_instance (task_instance_id)
) ENGINE=InnoDB COMMENT='节点实例表';

-- ============================================================
-- DQM - 数据质量模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dqm_quality_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    table_name VARCHAR(200),
    column_name VARCHAR(200),
    rule_type VARCHAR(50) COMMENT 'not_null/unique/range/regex/custom/referential/freshness',
    rule_expression TEXT,
    severity VARCHAR(20) DEFAULT 'warning' COMMENT 'info/warning/critical',
    enabled BOOLEAN DEFAULT TRUE,
    schedule_cron VARCHAR(100),
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB COMMENT='质量规则表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dqm_quality_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    rule_id BIGINT NOT NULL,
    check_time DATETIME,
    status VARCHAR(20) COMMENT 'pass/fail/error',
    total_rows BIGINT DEFAULT 0,
    failed_rows BIGINT DEFAULT 0,
    pass_rate DECIMAL(5,2),
    detail_json LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule (rule_id),
    INDEX idx_check_time (check_time)
) ENGINE=InnoDB COMMENT='质量报告表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dqm_quality_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    rule_id BIGINT NOT NULL,
    report_id BIGINT,
    alert_level VARCHAR(20) COMMENT 'info/warning/critical',
    message TEXT,
    status VARCHAR(20) DEFAULT 'open' COMMENT 'open/acknowledged/resolved',
    triggered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    INDEX idx_rule (rule_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='质量告警表';

-- ============================================================
-- DGV - 数据治理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dgv_meta_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    datasource_id BIGINT,
    schema_name VARCHAR(200),
    table_name VARCHAR(200) NOT NULL,
    table_type VARCHAR(20) DEFAULT 'table' COMMENT 'table/view',
    row_count BIGINT DEFAULT 0,
    data_size_bytes BIGINT DEFAULT 0,
    description TEXT,
    owner VARCHAR(100),
    last_sync_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_table_name (table_name)
) ENGINE=InnoDB COMMENT='元数据表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dgv_meta_column (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meta_table_id BIGINT NOT NULL,
    column_name VARCHAR(200) NOT NULL,
    column_type VARCHAR(100),
    is_nullable BOOLEAN DEFAULT TRUE,
    is_primary_key BOOLEAN DEFAULT FALSE,
    default_value VARCHAR(500),
    description TEXT,
    sort_order INT DEFAULT 0,
    INDEX idx_meta_table (meta_table_id)
) ENGINE=InnoDB COMMENT='元数据列表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dgv_data_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    source_table_id BIGINT NOT NULL,
    target_table_id BIGINT NOT NULL,
    transform_type VARCHAR(50) COMMENT 'ETL/SQL/API/STREAM',
    relationship_desc TEXT,
    workflow_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source (source_table_id),
    INDEX idx_target (target_table_id)
) ENGINE=InnoDB COMMENT='数据血缘表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dgv_data_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tag_type VARCHAR(50) COMMENT 'business/technical/security',
    color VARCHAR(20),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='数据标签表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dgv_table_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meta_table_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_table_tag (meta_table_id, tag_id),
    INDEX idx_tag (tag_id)
) ENGINE=InnoDB COMMENT='表-标签关联表';

-- ============================================================
-- DAS - 数据资产模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_das_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    table_name VARCHAR(200),
    asset_type VARCHAR(30) DEFAULT '表' COMMENT '表/视图/文件/API',
    category VARCHAR(30) DEFAULT '一般' COMMENT '核心/重要/一般',
    domain VARCHAR(50) COMMENT '业务域: 用户/订单/财务/运营',
    owner VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive/deprecated',
    access_level VARCHAR(30) DEFAULT 'internal' COMMENT 'public/internal/restricted/confidential',
    row_count BIGINT DEFAULT 0,
    data_size_bytes BIGINT DEFAULT 0,
    quality_score DECIMAL(5,2),
    last_profiled_at DATETIME,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain (domain),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_access_level (access_level)
) ENGINE=InnoDB COMMENT='数据资产表';

CREATE TABLE IF NOT EXISTS xnet_dataops_das_classification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    level VARCHAR(10) NOT NULL COMMENT 'L1/L2/L3',
    parent_id BIGINT DEFAULT 0,
    description TEXT,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB COMMENT='资产分类表';

CREATE TABLE IF NOT EXISTS xnet_dataops_das_access_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    asset_id BIGINT NOT NULL,
    user_id BIGINT,
    access_type VARCHAR(20) COMMENT 'read/write/download/query',
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    detail TEXT,
    INDEX idx_asset (asset_id),
    INDEX idx_user (user_id),
    INDEX idx_access_time (access_time)
) ENGINE=InnoDB COMMENT='资产访问记录表';

-- ============================================================
-- DAP - 数据API模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dap_api_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) DEFAULT 'GET' COMMENT 'GET/POST',
    datasource_id BIGINT,
    sql_content TEXT,
    param_config TEXT COMMENT 'JSON参数配置',
    description TEXT,
    status VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/published/deprecated',
    rate_limit INT DEFAULT 100 COMMENT '每分钟请求上限',
    cache_ttl INT DEFAULT 0 COMMENT '缓存时间(秒)',
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_path (path(200))
) ENGINE=InnoDB COMMENT='API配置表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dap_api_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    app_name VARCHAR(200) NOT NULL,
    api_key VARCHAR(128) NOT NULL UNIQUE,
    secret_key VARCHAR(128) NOT NULL,
    status VARCHAR(20) DEFAULT 'active' COMMENT 'active/revoked',
    permissions TEXT COMMENT 'JSON权限配置',
    expire_at DATETIME,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_key (api_key),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='API密钥表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dap_api_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_config_id BIGINT NOT NULL,
    api_key_id BIGINT,
    request_params TEXT,
    response_status INT,
    response_time BIGINT COMMENT '响应时间(ms)',
    ip_address VARCHAR(50),
    called_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_config (api_config_id),
    INDEX idx_called_at (called_at)
) ENGINE=InnoDB COMMENT='API调用日志表';

-- ============================================================
-- DMS - 数据脱敏模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dms_masking_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(30) NOT NULL COMMENT 'phone/email/idcard/name/address/bankcard/custom',
    mask_pattern VARCHAR(500),
    replacement VARCHAR(200),
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_type (rule_type)
) ENGINE=InnoDB COMMENT='脱敏规则表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dms_masking_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    table_name VARCHAR(200),
    column_name VARCHAR(200),
    rule_id BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_rule (rule_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB COMMENT='脱敏策略表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dms_masking_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    policy_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'running' COMMENT 'running/success/failed',
    total_rows BIGINT DEFAULT 0,
    masked_rows BIGINT DEFAULT 0,
    start_time DATETIME,
    end_time DATETIME,
    error_msg TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_policy (policy_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='脱敏任务日志表';

-- ============================================================
-- DOB - 数据可观测性模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dob_data_monitor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    datasource_id BIGINT,
    table_name VARCHAR(200),
    monitor_type VARCHAR(30) NOT NULL COMMENT 'freshness/volume/schema/custom',
    check_expression TEXT,
    threshold_value VARCHAR(200),
    alert_level VARCHAR(20) DEFAULT 'warning' COMMENT 'info/warning/critical',
    enabled BOOLEAN DEFAULT TRUE,
    schedule_cron VARCHAR(100),
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_type (monitor_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB COMMENT='数据监控表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dob_monitor_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    monitor_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL COMMENT 'anomaly/warning/normal',
    event_value VARCHAR(500),
    expected_value VARCHAR(500),
    message TEXT,
    status VARCHAR(20) DEFAULT 'open' COMMENT 'open/acknowledged/resolved',
    detected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    INDEX idx_monitor (monitor_id),
    INDEX idx_status (status),
    INDEX idx_detected_at (detected_at)
) ENGINE=InnoDB COMMENT='监控事件表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dob_data_sla (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    pipeline_name VARCHAR(200),
    expected_completion_time DATETIME,
    actual_completion_time DATETIME,
    sla_status VARCHAR(20) DEFAULT 'pending' COMMENT 'met/breached/pending',
    date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (sla_status),
    INDEX idx_date (date)
) ENGINE=InnoDB COMMENT='SLA管理表';

-- ============================================================
-- DAU - 数据审计模块
-- ============================================================

CREATE TABLE IF NOT EXISTS xnet_dataops_dau_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT,
    username VARCHAR(100),
    module VARCHAR(20) NOT NULL COMMENT 'DSM/DIM/DDV/TSK/DQM/DGV/DAS/DAP/DMS/DOB',
    action VARCHAR(20) NOT NULL COMMENT 'create/update/delete/query/export/login/logout',
    target_type VARCHAR(50),
    target_id VARCHAR(64),
    target_name VARCHAR(200),
    detail TEXT COMMENT 'JSON详情',
    ip_address VARCHAR(50),
    operate_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_operate_at (operate_at)
) ENGINE=InnoDB COMMENT='操作审计日志表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dau_data_change_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    datasource_id BIGINT,
    table_name VARCHAR(200),
    change_type VARCHAR(20) NOT NULL COMMENT 'insert/update/delete/truncate/ddl',
    affected_rows BIGINT DEFAULT 0,
    change_sql TEXT,
    changed_by VARCHAR(100),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id),
    INDEX idx_table (table_name),
    INDEX idx_type (change_type),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB COMMENT='数据变更记录表';

CREATE TABLE IF NOT EXISTS xnet_dataops_dau_compliance_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    report_type VARCHAR(30) NOT NULL COMMENT 'access/change/permission',
    period_start DATE,
    period_end DATE,
    total_events INT DEFAULT 0,
    risk_events INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'generated' COMMENT 'generated/reviewed/archived',
    generated_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (report_type),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='合规报告表';
