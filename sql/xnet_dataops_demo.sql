-- ============================================================
-- XnetDataops public showcase data
-- Safe to re-run: only rows with the demo-dataops marker are replaced.
-- Apply sql/xnet_dataops_ddl.sql before this file.
-- ============================================================

USE XnetDataops;
SET NAMES utf8mb4;
START TRANSACTION;

-- Remove the previous showcase dataset in dependency order.
DELETE FROM xnet_dataops_dim_field_mapping
WHERE task_id IN (SELECT id FROM xnet_dataops_dim_sync_task WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_dim_sync_log
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_tsk_node_instance
WHERE task_instance_id IN (SELECT id FROM xnet_dataops_tsk_task_instance WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_tsk_task_instance
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_tsk_workflow_edge
WHERE workflow_id IN (SELECT id FROM xnet_dataops_tsk_workflow WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_tsk_workflow_node
WHERE workflow_id IN (SELECT id FROM xnet_dataops_tsk_workflow WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_dqm_quality_alert
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dqm_quality_report
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dgv_table_tag
WHERE meta_table_id IN (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_dgv_data_lineage
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_das_access_record
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dap_api_call_log
WHERE api_config_id IN (SELECT id FROM xnet_dataops_dap_api_config WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_dms_masking_task_log
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dms_masking_policy
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dob_monitor_event
WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_usr_user_role
WHERE user_id IN (SELECT id FROM xnet_dataops_usr_user WHERE uid LIKE 'demo-dataops-%');

DELETE FROM xnet_dataops_dim_sync_task WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_ddv_sql_script WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_ddv_query_history WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_ddv_saved_query WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_tsk_workflow WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dqm_quality_rule WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dgv_meta_column
WHERE meta_table_id IN (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid LIKE 'demo-dataops-%');
DELETE FROM xnet_dataops_dgv_meta_table WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dgv_data_tag WHERE name LIKE '演示-%';
DELETE FROM xnet_dataops_das_asset WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_das_classification WHERE name LIKE '演示/%';
DELETE FROM xnet_dataops_dap_api_key WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dap_api_config WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dms_masking_rule WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dob_data_monitor WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dob_data_sla WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dau_audit_log WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dau_data_change_record WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dau_compliance_report WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_dsm_datasource WHERE uid LIKE 'demo-dataops-%';
DELETE FROM xnet_dataops_usr_user WHERE uid LIKE 'demo-dataops-%';

-- USR: demo tenant users and role mappings.
INSERT INTO xnet_dataops_usr_role (role_name, role_code, description)
VALUES
  ('管理员', 'ADMIN', '平台管理员，拥有全部演示模块权限'),
  ('数据开发', 'DEVELOPER', '负责数据接入、开发和任务调度'),
  ('数据分析师', 'ANALYST', '负责数据分析、质量和资产使用'),
  ('审计观察者', 'VIEWER', '只读查看治理、审计和运行状态')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description);

INSERT INTO xnet_dataops_usr_user
  (uid, username, password, email, phone, user_type, status, last_login_at, created_at)
VALUES
  ('demo-dataops-user-admin', 'demo_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6CQARaY1.k0YGKISbVFnTUjXS',
   'admin@demo.example', '12345678900', 'admin', 'active', NOW() - INTERVAL 8 MINUTE, NOW() - INTERVAL 120 DAY),
  ('demo-dataops-user-dev', 'demo_developer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6CQARaY1.k0YGKISbVFnTUjXS',
   'developer@demo.example', '12345678901', 'normal', 'active', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 95 DAY),
  ('demo-dataops-user-auditor', 'demo_auditor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6CQARaY1.k0YGKISbVFnTUjXS',
   'auditor@demo.example', '12345678902', 'readonly', 'active', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 80 DAY);

SET @dataops_admin = (SELECT id FROM xnet_dataops_usr_user WHERE uid = 'demo-dataops-user-admin');
SET @dataops_dev = (SELECT id FROM xnet_dataops_usr_user WHERE uid = 'demo-dataops-user-dev');
SET @dataops_auditor = (SELECT id FROM xnet_dataops_usr_user WHERE uid = 'demo-dataops-user-auditor');
SET @role_admin = (SELECT id FROM xnet_dataops_usr_role WHERE role_code = 'ADMIN');
SET @role_dev = (SELECT id FROM xnet_dataops_usr_role WHERE role_code = 'DEVELOPER');
SET @role_viewer = (SELECT id FROM xnet_dataops_usr_role WHERE role_code = 'VIEWER');

INSERT INTO xnet_dataops_usr_user_role (user_id, role_id, cluster_id)
VALUES
  (@dataops_admin, @role_admin, 1001),
  (@dataops_dev, @role_dev, 1001),
  (@dataops_auditor, @role_viewer, 1002);

-- DSM: heterogeneous but non-routable showcase sources.
INSERT INTO xnet_dataops_dsm_datasource
  (uid, name, type, host, port, database_name, username, encrypted_password,
   connection_params, status, last_test_at, description, created_by, created_at)
VALUES
  ('demo-dataops-ds-orders', '订单中心 MySQL', 'MYSQL', '192.168.100.21', 3306, 'orders', 'readonly_demo', NULL,
   '{"charset":"utf8mb4","ssl":false}', 'active', NOW() - INTERVAL 6 MINUTE, '订单交易主题数据源', 'demo_admin', NOW() - INTERVAL 90 DAY),
  ('demo-dataops-ds-warehouse', '分析仓库 PostgreSQL', 'POSTGRESQL', '192.168.100.31', 5432, 'analytics', 'etl_demo', NULL,
   '{"schema":"dwd","ssl":false}', 'active', NOW() - INTERVAL 11 MINUTE, '企业分析数仓演示连接', 'demo_admin', NOW() - INTERVAL 82 DAY),
  ('demo-dataops-ds-kafka', '实时事件 Kafka', 'KAFKA', 'kafka.demo.internal', 9092, 'business-events', 'consumer_demo', NULL,
   '{"groupId":"xnet-dataops-showcase"}', 'active', NOW() - INTERVAL 18 MINUTE, '实时业务事件接入', 'demo_developer', NOW() - INTERVAL 70 DAY),
  ('demo-dataops-ds-lake', '数据湖对象存储', 'S3', 's3.demo.internal', 443, 'xnet-demo-lake', 'lake_demo', NULL,
   '{"region":"demo-1","pathStyle":true}', 'inactive', NOW() - INTERVAL 2 DAY, '离线文件与归档数据', 'demo_developer', NOW() - INTERVAL 65 DAY);

SET @ds_orders = (SELECT id FROM xnet_dataops_dsm_datasource WHERE uid = 'demo-dataops-ds-orders');
SET @ds_warehouse = (SELECT id FROM xnet_dataops_dsm_datasource WHERE uid = 'demo-dataops-ds-warehouse');
SET @ds_kafka = (SELECT id FROM xnet_dataops_dsm_datasource WHERE uid = 'demo-dataops-ds-kafka');
SET @ds_lake = (SELECT id FROM xnet_dataops_dsm_datasource WHERE uid = 'demo-dataops-ds-lake');

-- DIM: batch and incremental integration jobs.
INSERT INTO xnet_dataops_dim_sync_task
  (uid, name, source_ds_id, target_ds_id, source_table, target_table, sync_mode,
   incremental_field, schedule_cron, status, description, created_by, created_at)
VALUES
  ('demo-dataops-sync-orders', '订单增量入仓', @ds_orders, @ds_warehouse, 'order_info', 'dwd_order_info', 'incremental',
   'updated_at', '0 */10 * * * ?', 'running', '每十分钟同步订单变化', 'demo_developer', NOW() - INTERVAL 60 DAY),
  ('demo-dataops-sync-users', '用户维度全量同步', @ds_orders, @ds_warehouse, 'customer_profile', 'dim_customer', 'full',
   NULL, '0 30 2 * * ?', 'running', '每日构建用户统一维度', 'demo_developer', NOW() - INTERVAL 55 DAY),
  ('demo-dataops-sync-events', '实时事件落湖', @ds_kafka, @ds_lake, 'topic_business_event', 'ods/business_event', 'incremental',
   'event_time', '0 */5 * * * ?', 'stopped', '实时事件按日期分区落湖', 'demo_admin', NOW() - INTERVAL 42 DAY);

SET @sync_orders = (SELECT id FROM xnet_dataops_dim_sync_task WHERE uid = 'demo-dataops-sync-orders');
SET @sync_users = (SELECT id FROM xnet_dataops_dim_sync_task WHERE uid = 'demo-dataops-sync-users');
SET @sync_events = (SELECT id FROM xnet_dataops_dim_sync_task WHERE uid = 'demo-dataops-sync-events');

INSERT INTO xnet_dataops_dim_field_mapping (task_id, source_field, target_field, transform_expression, sort_order)
VALUES
  (@sync_orders, 'id', 'order_id', NULL, 1),
  (@sync_orders, 'amount', 'order_amount', 'CAST(amount AS DECIMAL(18,2))', 2),
  (@sync_orders, 'updated_at', 'dw_updated_at', NULL, 3),
  (@sync_users, 'id', 'customer_id', NULL, 1),
  (@sync_users, 'phone', 'phone_masked', 'MASK_PHONE(phone)', 2),
  (@sync_events, 'event_json', 'payload', 'JSON_NORMALIZE(event_json)', 1);

INSERT INTO xnet_dataops_dim_sync_log
  (uid, task_id, start_time, end_time, status, rows_read, rows_written, error_msg, created_at)
VALUES
  ('demo-dataops-sync-log-1', @sync_orders, NOW() - INTERVAL 18 MINUTE, NOW() - INTERVAL 17 MINUTE, 'success', 12840, 12840, NULL, NOW() - INTERVAL 18 MINUTE),
  ('demo-dataops-sync-log-2', @sync_orders, NOW() - INTERVAL 8 MINUTE, NOW() - INTERVAL 7 MINUTE, 'success', 9362, 9362, NULL, NOW() - INTERVAL 8 MINUTE),
  ('demo-dataops-sync-log-3', @sync_users, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 9 HOUR, 'success', 482190, 482190, NULL, NOW() - INTERVAL 10 HOUR),
  ('demo-dataops-sync-log-4', @sync_events, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE, 'failed', 3200, 2874, '演示：目标分区写入超时', NOW() - INTERVAL 2 DAY);

-- DDV: scripts, saved queries and execution history.
INSERT INTO xnet_dataops_ddv_sql_script
  (uid, name, datasource_id, content, script_type, folder_path, status, created_by, created_at)
VALUES
  ('demo-dataops-script-gmv', '每日 GMV 汇总', @ds_warehouse, 'SELECT dt, SUM(order_amount) AS gmv FROM dwd_order_info GROUP BY dt;', 'sql', '/经营分析', 'published', 'demo_developer', NOW() - INTERVAL 40 DAY),
  ('demo-dataops-script-customer', '活跃客户分层', @ds_warehouse, 'SELECT customer_id, COUNT(*) AS order_count FROM dwd_order_info GROUP BY customer_id;', 'sql', '/用户增长', 'published', 'demo_developer', NOW() - INTERVAL 32 DAY),
  ('demo-dataops-script-quality', '订单字段质量检查', @ds_warehouse, 'SELECT COUNT(*) FROM dwd_order_info WHERE order_id IS NULL;', 'sql', '/质量检查', 'draft', 'demo_developer', NOW() - INTERVAL 20 DAY),
  ('demo-dataops-script-export', '周报指标导出', @ds_warehouse, 'SELECT * FROM ads_weekly_operation WHERE week_id = CURRENT_DATE;', 'sql', '/运营报表', 'published', 'demo_admin', NOW() - INTERVAL 16 DAY);

INSERT INTO xnet_dataops_ddv_query_history
  (uid, datasource_id, sql_content, execute_status, rows_affected, duration_ms, error_msg, executed_by, executed_at)
VALUES
  ('demo-dataops-query-1', @ds_warehouse, 'SELECT COUNT(*) FROM dwd_order_info;', 'success', 1, 186, NULL, 'demo_developer', NOW() - INTERVAL 12 MINUTE),
  ('demo-dataops-query-2', @ds_warehouse, 'SELECT * FROM ads_daily_gmv ORDER BY dt DESC LIMIT 30;', 'success', 30, 243, NULL, 'demo_developer', NOW() - INTERVAL 1 HOUR),
  ('demo-dataops-query-3', @ds_orders, 'SELECT status, COUNT(*) FROM order_info GROUP BY status;', 'success', 6, 92, NULL, 'demo_admin', NOW() - INTERVAL 3 HOUR),
  ('demo-dataops-query-4', @ds_warehouse, 'SELECT * FROM dim_customer WHERE customer_level = ''VIP'';', 'success', 128, 310, NULL, 'demo_developer', NOW() - INTERVAL 8 HOUR),
  ('demo-dataops-query-5', @ds_lake, 'SELECT * FROM missing_partition;', 'failed', 0, 520, '演示：分区尚未生成', 'demo_developer', NOW() - INTERVAL 1 DAY);

INSERT INTO xnet_dataops_ddv_saved_query
  (uid, name, datasource_id, sql_content, description, created_by)
VALUES
  ('demo-dataops-saved-1', '近 7 日销售趋势', @ds_warehouse, 'SELECT dt, gmv FROM ads_daily_gmv WHERE dt >= CURRENT_DATE - INTERVAL 7 DAY;', '经营看板常用查询', 'demo_developer'),
  ('demo-dataops-saved-2', '异常订单明细', @ds_warehouse, 'SELECT * FROM dwd_order_info WHERE quality_flag = 0;', '数据质量排查查询', 'demo_admin');

-- TSK: scheduled DAGs with recent instances.
INSERT INTO xnet_dataops_tsk_workflow
  (uid, name, description, schedule_cron, status, dag_json, created_by, created_at)
VALUES
  ('demo-dataops-workflow-daily', '每日经营指标流水线', '完成订单同步、质量校验和指标聚合', '0 15 3 * * ?', 'online',
   '{"nodes":[{"id":"sync","name":"订单同步"},{"id":"quality","name":"质量校验"},{"id":"aggregate","name":"指标聚合"}],"edges":[["sync","quality"],["quality","aggregate"]]}',
   'demo_developer', NOW() - INTERVAL 50 DAY),
  ('demo-dataops-workflow-customer', '用户画像周更', '每周更新客户标签和活跃度分层', '0 0 4 ? * MON', 'online',
   '{"nodes":[{"id":"profile","name":"画像计算"},{"id":"publish","name":"标签发布"}],"edges":[["profile","publish"]]}',
   'demo_developer', NOW() - INTERVAL 35 DAY),
  ('demo-dataops-workflow-archive', '历史数据归档', '按月将冷数据归档到对象存储', '0 0 1 1 * ?', 'offline',
   '{"nodes":[{"id":"archive","name":"归档任务"}],"edges":[]}', 'demo_admin', NOW() - INTERVAL 28 DAY);

SET @wf_daily = (SELECT id FROM xnet_dataops_tsk_workflow WHERE uid = 'demo-dataops-workflow-daily');
SET @wf_customer = (SELECT id FROM xnet_dataops_tsk_workflow WHERE uid = 'demo-dataops-workflow-customer');

INSERT INTO xnet_dataops_tsk_workflow_node
  (workflow_id, node_key, node_name, node_type, config_json, position_x, position_y)
VALUES
  (@wf_daily, 'sync', '订单增量同步', 'sync', CONCAT('{"taskId":', @sync_orders, '}'), 80, 120),
  (@wf_daily, 'quality', '订单质量校验', 'sql', '{"script":"订单字段质量检查"}', 330, 120),
  (@wf_daily, 'aggregate', '经营指标聚合', 'sql', '{"script":"每日 GMV 汇总"}', 580, 120),
  (@wf_customer, 'profile', '客户画像计算', 'sql', '{"script":"活跃客户分层"}', 120, 160),
  (@wf_customer, 'publish', '画像标签发布', 'http', '{"endpoint":"https://api.demo.example/profile/publish"}', 430, 160);

INSERT INTO xnet_dataops_tsk_workflow_edge (workflow_id, source_node_key, target_node_key)
VALUES
  (@wf_daily, 'sync', 'quality'),
  (@wf_daily, 'quality', 'aggregate'),
  (@wf_customer, 'profile', 'publish');

INSERT INTO xnet_dataops_tsk_task_instance
  (uid, workflow_id, status, trigger_type, start_time, end_time, created_at)
VALUES
  ('demo-dataops-instance-1', @wf_daily, 'success', 'scheduled', NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 7 HOUR),
  ('demo-dataops-instance-2', @wf_daily, 'success', 'manual', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 16 MINUTE, NOW() - INTERVAL 1 DAY),
  ('demo-dataops-instance-3', @wf_customer, 'success', 'scheduled', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 42 MINUTE, NOW() - INTERVAL 3 DAY),
  ('demo-dataops-instance-4', @wf_daily, 'failed', 'api', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 4 MINUTE, NOW() - INTERVAL 5 DAY);

SET @inst_1 = (SELECT id FROM xnet_dataops_tsk_task_instance WHERE uid = 'demo-dataops-instance-1');
SET @inst_4 = (SELECT id FROM xnet_dataops_tsk_task_instance WHERE uid = 'demo-dataops-instance-4');
INSERT INTO xnet_dataops_tsk_node_instance
  (task_instance_id, node_key, status, start_time, end_time, log_content)
VALUES
  (@inst_1, 'sync', 'success', NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 7 HOUR + INTERVAL 8 MINUTE, '读取 12,840 行，写入 12,840 行'),
  (@inst_1, 'quality', 'success', NOW() - INTERVAL 7 HOUR + INTERVAL 8 MINUTE, NOW() - INTERVAL 7 HOUR + INTERVAL 11 MINUTE, '质量通过率 99.82%'),
  (@inst_1, 'aggregate', 'success', NOW() - INTERVAL 7 HOUR + INTERVAL 11 MINUTE, NOW() - INTERVAL 7 HOUR + INTERVAL 15 MINUTE, '生成 24 个经营指标'),
  (@inst_4, 'sync', 'success', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 3 MINUTE, '读取 3,100 行'),
  (@inst_4, 'quality', 'failed', NOW() - INTERVAL 5 DAY + INTERVAL 3 MINUTE, NOW() - INTERVAL 5 DAY + INTERVAL 4 MINUTE, '演示：订单金额范围检查未通过');

-- DQM: rules, reports and actionable alerts.
INSERT INTO xnet_dataops_dqm_quality_rule
  (uid, name, datasource_id, table_name, column_name, rule_type, rule_expression,
   severity, enabled, schedule_cron, description, created_by)
VALUES
  ('demo-dataops-quality-order-id', '订单主键非空', @ds_warehouse, 'dwd_order_info', 'order_id', 'not_null', 'order_id IS NOT NULL', 'critical', 1, '0 */10 * * * ?', '保证订单主键完整', 'demo_admin'),
  ('demo-dataops-quality-amount', '订单金额合理范围', @ds_warehouse, 'dwd_order_info', 'order_amount', 'range', 'order_amount BETWEEN 0 AND 1000000', 'warning', 1, '0 20 * * * ?', '识别异常交易金额', 'demo_developer'),
  ('demo-dataops-quality-phone', '客户手机号格式', @ds_warehouse, 'dim_customer', 'phone_masked', 'regex', '^1[0-9]{10}$', 'warning', 1, '0 30 3 * * ?', '检查手机号格式后再脱敏', 'demo_developer'),
  ('demo-dataops-quality-freshness', '经营指标新鲜度', @ds_warehouse, 'ads_daily_gmv', 'dt', 'freshness', 'MAX(dt) >= CURRENT_DATE - INTERVAL 1 DAY', 'critical', 1, '0 0 8 * * ?', '确保日报按时产出', 'demo_admin');

SET @qr_id = (SELECT id FROM xnet_dataops_dqm_quality_rule WHERE uid = 'demo-dataops-quality-order-id');
SET @qr_amount = (SELECT id FROM xnet_dataops_dqm_quality_rule WHERE uid = 'demo-dataops-quality-amount');
SET @qr_fresh = (SELECT id FROM xnet_dataops_dqm_quality_rule WHERE uid = 'demo-dataops-quality-freshness');

INSERT INTO xnet_dataops_dqm_quality_report
  (uid, rule_id, check_time, status, total_rows, failed_rows, pass_rate, detail_json)
VALUES
  ('demo-dataops-report-1', @qr_id, NOW() - INTERVAL 20 MINUTE, 'pass', 12840, 0, 100.00, '{"summary":"全部订单主键有效"}'),
  ('demo-dataops-report-2', @qr_amount, NOW() - INTERVAL 18 MINUTE, 'pass', 12840, 23, 99.82, '{"min":1.20,"max":682400.00}'),
  ('demo-dataops-report-3', @qr_fresh, NOW() - INTERVAL 2 HOUR, 'pass', 1, 0, 100.00, '{"latestPartition":"today"}'),
  ('demo-dataops-report-4', @qr_amount, NOW() - INTERVAL 1 DAY, 'fail', 12011, 142, 98.82, '{"reason":"发现异常负数金额"}');

SET @report_4 = (SELECT id FROM xnet_dataops_dqm_quality_report WHERE uid = 'demo-dataops-report-4');
INSERT INTO xnet_dataops_dqm_quality_alert
  (uid, rule_id, report_id, alert_level, message, status, triggered_at, resolved_at)
VALUES
  ('demo-dataops-quality-alert-1', @qr_amount, @report_4, 'warning', '昨日批次发现 142 条金额异常记录，已隔离复核', 'acknowledged', NOW() - INTERVAL 1 DAY, NULL),
  ('demo-dataops-quality-alert-2', @qr_fresh, NULL, 'critical', '三日前经营日报延迟 26 分钟，现已恢复', 'resolved', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 26 MINUTE);

-- DGV: catalog, columns, lineage and tags.
INSERT INTO xnet_dataops_dgv_meta_table
  (uid, datasource_id, schema_name, table_name, table_type, row_count, data_size_bytes,
   description, owner, last_sync_at)
VALUES
  ('demo-dataops-meta-order-source', @ds_orders, 'orders', 'order_info', 'table', 12840920, 4294967296, '订单中心交易明细', '交易平台组', NOW() - INTERVAL 9 MINUTE),
  ('demo-dataops-meta-order-dwd', @ds_warehouse, 'dwd', 'dwd_order_info', 'table', 12698110, 7516192768, '清洗后的订单事实表', '数据工程组', NOW() - INTERVAL 7 MINUTE),
  ('demo-dataops-meta-customer', @ds_warehouse, 'dim', 'dim_customer', 'table', 482190, 536870912, '客户统一维度', '用户增长组', NOW() - INTERVAL 15 MINUTE),
  ('demo-dataops-meta-gmv', @ds_warehouse, 'ads', 'ads_daily_gmv', 'table', 730, 10485760, '每日经营指标汇总', '经营分析组', NOW() - INTERVAL 12 MINUTE),
  ('demo-dataops-meta-report', @ds_warehouse, 'ads', 'vw_operation_dashboard', 'view', 90, 1048576, '经营驾驶舱查询视图', '经营分析组', NOW() - INTERVAL 20 MINUTE);

SET @meta_source = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'demo-dataops-meta-order-source');
SET @meta_dwd = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'demo-dataops-meta-order-dwd');
SET @meta_customer = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'demo-dataops-meta-customer');
SET @meta_gmv = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'demo-dataops-meta-gmv');
SET @meta_report = (SELECT id FROM xnet_dataops_dgv_meta_table WHERE uid = 'demo-dataops-meta-report');

INSERT INTO xnet_dataops_dgv_meta_column
  (meta_table_id, column_name, column_type, is_nullable, is_primary_key, default_value, description, sort_order)
VALUES
  (@meta_source, 'id', 'BIGINT', 0, 1, NULL, '订单主键', 1),
  (@meta_source, 'customer_id', 'BIGINT', 0, 0, NULL, '客户标识', 2),
  (@meta_source, 'amount', 'DECIMAL(18,2)', 0, 0, '0', '订单金额', 3),
  (@meta_source, 'updated_at', 'DATETIME', 0, 0, NULL, '更新时间', 4),
  (@meta_dwd, 'order_id', 'BIGINT', 0, 1, NULL, '标准订单标识', 1),
  (@meta_dwd, 'order_amount', 'DECIMAL(18,2)', 0, 0, '0', '标准订单金额', 2),
  (@meta_dwd, 'quality_flag', 'TINYINT', 0, 0, '1', '质量标记', 3),
  (@meta_customer, 'customer_id', 'BIGINT', 0, 1, NULL, '客户标识', 1),
  (@meta_customer, 'customer_level', 'VARCHAR(20)', 1, 0, NULL, '客户等级', 2),
  (@meta_gmv, 'dt', 'DATE', 0, 1, NULL, '统计日期', 1),
  (@meta_gmv, 'gmv', 'DECIMAL(20,2)', 0, 0, '0', '成交总额', 2),
  (@meta_report, 'metric_name', 'VARCHAR(100)', 0, 0, NULL, '指标名称', 1),
  (@meta_report, 'metric_value', 'DECIMAL(20,4)', 1, 0, NULL, '指标值', 2);

INSERT INTO xnet_dataops_dgv_data_lineage
  (uid, source_table_id, target_table_id, transform_type, relationship_desc, workflow_id)
VALUES
  ('demo-dataops-lineage-1', @meta_source, @meta_dwd, 'ETL', '订单增量清洗与标准化', @wf_daily),
  ('demo-dataops-lineage-2', @meta_dwd, @meta_gmv, 'SQL', '按日期聚合成交指标', @wf_daily),
  ('demo-dataops-lineage-3', @meta_customer, @meta_report, 'SQL', '客户分层指标汇总', @wf_customer),
  ('demo-dataops-lineage-4', @meta_gmv, @meta_report, 'API', '经营驾驶舱指标服务', @wf_daily);

INSERT INTO xnet_dataops_dgv_data_tag (name, tag_type, color, description)
VALUES
  ('演示-核心业务', 'business', '#1677ff', '核心经营与交易数据'),
  ('演示-个人信息', 'security', '#f5222d', '包含需要保护的个人信息'),
  ('演示-已认证', 'technical', '#52c41a', '通过质量校验并完成资产认证');
SET @tag_core = (SELECT id FROM xnet_dataops_dgv_data_tag WHERE name = '演示-核心业务' LIMIT 1);
SET @tag_pii = (SELECT id FROM xnet_dataops_dgv_data_tag WHERE name = '演示-个人信息' LIMIT 1);
SET @tag_cert = (SELECT id FROM xnet_dataops_dgv_data_tag WHERE name = '演示-已认证' LIMIT 1);
INSERT INTO xnet_dataops_dgv_table_tag (meta_table_id, tag_id)
VALUES
  (@meta_source, @tag_core),
  (@meta_customer, @tag_pii),
  (@meta_dwd, @tag_cert),
  (@meta_gmv, @tag_core),
  (@meta_gmv, @tag_cert);

-- DAS: certified assets and access records.
INSERT INTO xnet_dataops_das_asset
  (uid, name, datasource_id, table_name, asset_type, category, domain, owner, description,
   status, access_level, row_count, data_size_bytes, quality_score, last_profiled_at, created_by)
VALUES
  ('demo-dataops-asset-orders', '订单交易明细', @ds_warehouse, 'dwd_order_info', '表', '核心', '交易', '交易平台组', '统一订单事实资产', 'active', 'restricted', 12698110, 7516192768, 99.82, NOW() - INTERVAL 18 MINUTE, 'demo_admin'),
  ('demo-dataops-asset-customer', '客户统一画像', @ds_warehouse, 'dim_customer', '表', '核心', '用户', '用户增长组', '客户标签与分层资产', 'active', 'confidential', 482190, 536870912, 98.60, NOW() - INTERVAL 30 MINUTE, 'demo_admin'),
  ('demo-dataops-asset-gmv', '每日 GMV 指标', @ds_warehouse, 'ads_daily_gmv', '表', '重要', '经营', '经营分析组', '经营看板核心指标', 'active', 'internal', 730, 10485760, 100.00, NOW() - INTERVAL 25 MINUTE, 'demo_developer'),
  ('demo-dataops-asset-dashboard', '经营驾驶舱视图', @ds_warehouse, 'vw_operation_dashboard', '视图', '重要', '经营', '经营分析组', '管理驾驶舱统一查询视图', 'active', 'internal', 90, 1048576, 99.50, NOW() - INTERVAL 40 MINUTE, 'demo_developer'),
  ('demo-dataops-asset-event', '实时业务事件', @ds_lake, 'ods/business_event', '文件', '一般', '运营', '数据工程组', '按天归档的实时事件', 'active', 'internal', 38500000, 21474836480, 96.70, NOW() - INTERVAL 2 HOUR, 'demo_developer');

SET @asset_orders = (SELECT id FROM xnet_dataops_das_asset WHERE uid = 'demo-dataops-asset-orders');
SET @asset_customer = (SELECT id FROM xnet_dataops_das_asset WHERE uid = 'demo-dataops-asset-customer');
SET @asset_gmv = (SELECT id FROM xnet_dataops_das_asset WHERE uid = 'demo-dataops-asset-gmv');

INSERT INTO xnet_dataops_das_classification (name, level, parent_id, description, sort_order)
VALUES
  ('演示/业务数据', 'L1', 0, '按业务主题组织的数据资产', 1),
  ('演示/交易域', 'L2', 0, '订单、支付和履约主题', 10),
  ('演示/用户域', 'L2', 0, '客户与会员主题', 20),
  ('演示/经营指标', 'L2', 0, '经营分析指标与报表', 30);

INSERT INTO xnet_dataops_das_access_record
  (uid, asset_id, user_id, access_type, access_time, ip_address, detail)
VALUES
  ('demo-dataops-access-1', @asset_orders, @dataops_dev, 'query', NOW() - INTERVAL 16 MINUTE, '192.168.100.81', '{"purpose":"质量分析"}'),
  ('demo-dataops-access-2', @asset_gmv, @dataops_admin, 'read', NOW() - INTERVAL 1 HOUR, '192.168.100.82', '{"purpose":"经营看板"}'),
  ('demo-dataops-access-3', @asset_customer, @dataops_auditor, 'read', NOW() - INTERVAL 5 HOUR, '192.168.100.83', '{"purpose":"权限审计"}'),
  ('demo-dataops-access-4', @asset_orders, @dataops_dev, 'export', NOW() - INTERVAL 1 DAY, '192.168.100.81', '{"rows":2000,"masked":true}');

-- DAP: published APIs, keys and call metrics.
INSERT INTO xnet_dataops_dap_api_config
  (uid, name, path, method, datasource_id, sql_content, param_config, description,
   status, rate_limit, cache_ttl, created_by)
VALUES
  ('demo-dataops-api-order', '订单详情查询', '/api/v1/orders/:orderId', 'GET', @ds_warehouse, 'SELECT * FROM dwd_order_info WHERE order_id = :orderId', '[{"name":"orderId","type":"long","required":true}]', '按订单标识查询订单详情', 'published', 300, 60, 'demo_developer'),
  ('demo-dataops-api-customer', '客户画像摘要', '/api/v1/customers/:customerId/profile', 'GET', @ds_warehouse, 'SELECT * FROM dim_customer WHERE customer_id = :customerId', '[{"name":"customerId","type":"long","required":true}]', '返回脱敏后的客户画像', 'published', 120, 300, 'demo_developer'),
  ('demo-dataops-api-gmv', '经营指标趋势', '/api/v1/metrics/gmv', 'GET', @ds_warehouse, 'SELECT dt, gmv FROM ads_daily_gmv WHERE dt BETWEEN :start AND :end', '[{"name":"start","type":"date"},{"name":"end","type":"date"}]', '经营驾驶舱趋势接口', 'published', 600, 120, 'demo_admin'),
  ('demo-dataops-api-quality', '质量报告查询', '/api/v1/quality/reports', 'POST', @ds_warehouse, 'SELECT * FROM quality_report WHERE check_time >= :since', '[{"name":"since","type":"datetime"}]', '内部质量平台接口', 'draft', 60, 0, 'demo_admin');

SET @api_order = (SELECT id FROM xnet_dataops_dap_api_config WHERE uid = 'demo-dataops-api-order');
SET @api_customer = (SELECT id FROM xnet_dataops_dap_api_config WHERE uid = 'demo-dataops-api-customer');
SET @api_gmv = (SELECT id FROM xnet_dataops_dap_api_config WHERE uid = 'demo-dataops-api-gmv');

INSERT INTO xnet_dataops_dap_api_key
  (uid, app_name, api_key, secret_key, status, permissions, expire_at, created_by)
VALUES
  ('demo-dataops-key-dashboard', '经营驾驶舱', 'demo-key-dashboard-not-valid', 'demo-secret-not-valid', 'active', '["metrics:read","orders:read"]', NOW() + INTERVAL 180 DAY, 'demo_admin'),
  ('demo-dataops-key-quality', '质量中心', 'demo-key-quality-not-valid', 'demo-secret-not-valid-2', 'active', '["quality:read"]', NOW() + INTERVAL 90 DAY, 'demo_admin');
SET @key_dashboard = (SELECT id FROM xnet_dataops_dap_api_key WHERE uid = 'demo-dataops-key-dashboard');
SET @key_quality = (SELECT id FROM xnet_dataops_dap_api_key WHERE uid = 'demo-dataops-key-quality');

INSERT INTO xnet_dataops_dap_api_call_log
  (api_config_id, api_key_id, request_params, response_status, response_time, ip_address, called_at)
VALUES
  (@api_gmv, @key_dashboard, '{"start":"2026-07-01","end":"2026-07-21"}', 200, 84, '192.168.100.91', NOW() - INTERVAL 4 MINUTE),
  (@api_order, @key_dashboard, '{"orderId":10002881}', 200, 42, '192.168.100.91', NOW() - INTERVAL 7 MINUTE),
  (@api_customer, @key_dashboard, '{"customerId":88019}', 200, 66, '192.168.100.91', NOW() - INTERVAL 12 MINUTE),
  (@api_gmv, @key_dashboard, '{"start":"2026-07-14","end":"2026-07-21"}', 200, 78, '192.168.100.92', NOW() - INTERVAL 28 MINUTE),
  (@api_order, @key_dashboard, '{"orderId":0}', 404, 31, '192.168.100.93', NOW() - INTERVAL 1 HOUR),
  (@api_gmv, @key_quality, '{"start":"2026-07-20","end":"2026-07-21"}', 403, 18, '192.168.100.94', NOW() - INTERVAL 2 HOUR);

-- DMS: masking rules, policies and completed jobs.
INSERT INTO xnet_dataops_dms_masking_rule
  (uid, name, rule_type, mask_pattern, replacement, description, created_by)
VALUES
  ('demo-dataops-mask-phone', '手机号中间四位脱敏', 'phone', '(\\d{3})\\d{4}(\\d{4})', '$1****$2', '适用于客户联系方式', 'demo_admin'),
  ('demo-dataops-mask-email', '邮箱账号脱敏', 'email', '(^.).*(@.*$)', '$1***$2', '保留邮箱首字母和域名', 'demo_admin'),
  ('demo-dataops-mask-idcard', '身份证号脱敏', 'idcard', '(\\d{6})\\d{8}(\\w{4})', '$1********$2', '保护证件敏感信息', 'demo_admin'),
  ('demo-dataops-mask-name', '姓名泛化', 'name', '(.).*', '$1**', '仅保留姓氏', 'demo_auditor');

SET @mask_phone = (SELECT id FROM xnet_dataops_dms_masking_rule WHERE uid = 'demo-dataops-mask-phone');
SET @mask_email = (SELECT id FROM xnet_dataops_dms_masking_rule WHERE uid = 'demo-dataops-mask-email');
SET @mask_id = (SELECT id FROM xnet_dataops_dms_masking_rule WHERE uid = 'demo-dataops-mask-idcard');

INSERT INTO xnet_dataops_dms_masking_policy
  (uid, name, datasource_id, table_name, column_name, rule_id, enabled, priority, description, created_by)
VALUES
  ('demo-dataops-policy-phone', '客户手机号保护', @ds_warehouse, 'dim_customer', 'phone', @mask_phone, 1, 100, '查询和导出场景自动脱敏', 'demo_admin'),
  ('demo-dataops-policy-email', '客户邮箱保护', @ds_warehouse, 'dim_customer', 'email', @mask_email, 1, 90, '邮箱账号字段脱敏', 'demo_admin'),
  ('demo-dataops-policy-id', '证件号码保护', @ds_warehouse, 'dim_customer', 'id_card', @mask_id, 1, 110, '严格保护身份凭证', 'demo_auditor');

SET @policy_phone = (SELECT id FROM xnet_dataops_dms_masking_policy WHERE uid = 'demo-dataops-policy-phone');
SET @policy_email = (SELECT id FROM xnet_dataops_dms_masking_policy WHERE uid = 'demo-dataops-policy-email');
INSERT INTO xnet_dataops_dms_masking_task_log
  (uid, policy_id, status, total_rows, masked_rows, start_time, end_time, error_msg)
VALUES
  ('demo-dataops-mask-log-1', @policy_phone, 'success', 482190, 482190, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR + INTERVAL 8 MINUTE, NULL),
  ('demo-dataops-mask-log-2', @policy_email, 'success', 481220, 481220, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 7 MINUTE, NULL),
  ('demo-dataops-mask-log-3', @policy_phone, 'success', 12680, 12680, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE, NULL);

-- DOB: freshness, volume and schema monitors plus SLA.
INSERT INTO xnet_dataops_dob_data_monitor
  (uid, name, datasource_id, table_name, monitor_type, check_expression, threshold_value,
   alert_level, enabled, schedule_cron, description, created_by)
VALUES
  ('demo-dataops-monitor-fresh', '订单表新鲜度', @ds_warehouse, 'dwd_order_info', 'freshness', 'MAX(dw_updated_at)', '15m', 'critical', 1, '0 */5 * * * ?', '超过十五分钟无更新则告警', 'demo_admin'),
  ('demo-dataops-monitor-volume', '每日订单量波动', @ds_warehouse, 'dwd_order_info', 'volume', 'COUNT(*) WHERE dt = CURRENT_DATE', '±30%', 'warning', 1, '0 0 8 * * ?', '监测每日订单量异常波动', 'demo_developer'),
  ('demo-dataops-monitor-schema', '客户表结构变化', @ds_warehouse, 'dim_customer', 'schema', 'SCHEMA_HASH(dim_customer)', 'unchanged', 'warning', 1, '0 */30 * * * ?', '监测字段新增、删除和类型变化', 'demo_admin'),
  ('demo-dataops-monitor-gmv', 'GMV 指标完整性', @ds_warehouse, 'ads_daily_gmv', 'custom', 'gmv IS NOT NULL', '100%', 'critical', 1, '0 10 8 * * ?', '保证驾驶舱核心指标可用', 'demo_admin');

SET @monitor_fresh = (SELECT id FROM xnet_dataops_dob_data_monitor WHERE uid = 'demo-dataops-monitor-fresh');
SET @monitor_volume = (SELECT id FROM xnet_dataops_dob_data_monitor WHERE uid = 'demo-dataops-monitor-volume');
SET @monitor_schema = (SELECT id FROM xnet_dataops_dob_data_monitor WHERE uid = 'demo-dataops-monitor-schema');
INSERT INTO xnet_dataops_dob_monitor_event
  (uid, monitor_id, event_type, event_value, expected_value, message, status, detected_at, resolved_at)
VALUES
  ('demo-dataops-event-1', @monitor_fresh, 'normal', '6m', '<=15m', '订单增量链路运行正常', 'resolved', NOW() - INTERVAL 12 MINUTE, NOW() - INTERVAL 11 MINUTE),
  ('demo-dataops-event-2', @monitor_volume, 'warning', '-34%', '±30%', '早间订单量低于近七日均值', 'acknowledged', NOW() - INTERVAL 2 HOUR, NULL),
  ('demo-dataops-event-3', @monitor_schema, 'anomaly', '新增字段 channel_code', 'unchanged', '检测到客户表新增渠道字段', 'resolved', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 35 MINUTE);

INSERT INTO xnet_dataops_dob_data_sla
  (uid, name, pipeline_name, expected_completion_time, actual_completion_time, sla_status, date)
VALUES
  ('demo-dataops-sla-1', '每日经营指标 08:00 前就绪', '每日经营指标流水线', CURRENT_DATE + INTERVAL 8 HOUR, CURRENT_DATE + INTERVAL 7 HOUR + INTERVAL 42 MINUTE, 'met', CURRENT_DATE),
  ('demo-dataops-sla-2', '用户画像周一 06:00 前就绪', '用户画像周更', CURRENT_DATE - INTERVAL WEEKDAY(CURRENT_DATE) DAY + INTERVAL 6 HOUR, CURRENT_DATE - INTERVAL WEEKDAY(CURRENT_DATE) DAY + INTERVAL 5 HOUR + INTERVAL 35 MINUTE, 'met', CURRENT_DATE - INTERVAL WEEKDAY(CURRENT_DATE) DAY),
  ('demo-dataops-sla-3', '订单增量延迟低于 15 分钟', '订单增量入仓', NOW() + INTERVAL 15 MINUTE, NOW() + INTERVAL 6 MINUTE, 'met', CURRENT_DATE),
  ('demo-dataops-sla-4', '月度归档 03:00 前完成', '历史数据归档', DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') + INTERVAL 3 HOUR, NULL, 'pending', DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'));

-- DAU: audit, data change and compliance reports.
INSERT INTO xnet_dataops_dau_audit_log
  (uid, user_id, username, module, action, target_type, target_id, target_name, detail, ip_address, operate_at)
VALUES
  ('demo-dataops-audit-1', @dataops_admin, 'demo_admin', 'DSM', 'create', 'datasource', CAST(@ds_warehouse AS CHAR), '分析仓库 PostgreSQL', '{"result":"success"}', '192.168.100.82', NOW() - INTERVAL 82 DAY),
  ('demo-dataops-audit-2', @dataops_dev, 'demo_developer', 'DIM', 'update', 'sync_task', CAST(@sync_orders AS CHAR), '订单增量入仓', '{"schedule":"every 10 minutes"}', '192.168.100.81', NOW() - INTERVAL 3 HOUR),
  ('demo-dataops-audit-3', @dataops_dev, 'demo_developer', 'DDV', 'query', 'sql_script', NULL, '每日 GMV 汇总', '{"durationMs":186}', '192.168.100.81', NOW() - INTERVAL 1 HOUR),
  ('demo-dataops-audit-4', @dataops_admin, 'demo_admin', 'DQM', 'create', 'quality_rule', CAST(@qr_fresh AS CHAR), '经营指标新鲜度', '{"severity":"critical"}', '192.168.100.82', NOW() - INTERVAL 2 DAY),
  ('demo-dataops-audit-5', @dataops_auditor, 'demo_auditor', 'DAS', 'export', 'asset', CAST(@asset_orders AS CHAR), '订单交易明细', '{"rows":2000,"masked":true}', '192.168.100.83', NOW() - INTERVAL 1 DAY),
  ('demo-dataops-audit-6', @dataops_admin, 'demo_admin', 'DAP', 'update', 'api_config', CAST(@api_gmv AS CHAR), '经营指标趋势', '{"rateLimit":600}', '192.168.100.82', NOW() - INTERVAL 28 MINUTE);

INSERT INTO xnet_dataops_dau_data_change_record
  (uid, datasource_id, table_name, change_type, affected_rows, change_sql, changed_by, changed_at)
VALUES
  ('demo-dataops-change-1', @ds_warehouse, 'dwd_order_info', 'insert', 12840, 'INSERT INTO dwd_order_info SELECT ...', '每日经营指标流水线', NOW() - INTERVAL 18 MINUTE),
  ('demo-dataops-change-2', @ds_warehouse, 'dim_customer', 'update', 482190, 'MERGE INTO dim_customer ...', '用户画像周更', NOW() - INTERVAL 3 DAY),
  ('demo-dataops-change-3', @ds_warehouse, 'dim_customer', 'ddl', 0, 'ALTER TABLE dim_customer ADD COLUMN channel_code VARCHAR(20)', 'demo_developer', NOW() - INTERVAL 2 DAY),
  ('demo-dataops-change-4', @ds_lake, 'ods/business_event', 'insert', 3850000, 'WRITE PARTITION dt = CURRENT_DATE', '实时事件落湖', NOW() - INTERVAL 1 HOUR);

INSERT INTO xnet_dataops_dau_compliance_report
  (uid, name, report_type, period_start, period_end, total_events, risk_events, status, generated_by, created_at)
VALUES
  ('demo-dataops-compliance-1', '2026 年 7 月数据访问审计', 'access', '2026-07-01', '2026-07-31', 18642, 12, 'generated', 'demo_auditor', NOW() - INTERVAL 1 DAY),
  ('demo-dataops-compliance-2', '2026 年第二季度数据变更报告', 'change', '2026-04-01', '2026-06-30', 5280, 8, 'reviewed', 'demo_auditor', NOW() - INTERVAL 15 DAY),
  ('demo-dataops-compliance-3', '敏感资产权限复核', 'permission', '2026-07-01', '2026-07-21', 326, 3, 'reviewed', 'demo_admin', NOW() - INTERVAL 6 HOUR);

COMMIT;
