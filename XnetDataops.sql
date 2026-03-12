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
