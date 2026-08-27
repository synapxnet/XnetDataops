-- GOAI 1.0.0 固定演示 Fixture。数据进入真实业务表，Service 不硬编码指标。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+00:00';

INSERT INTO xnet_dataops_dsm_datasource
(id, uid, name, type, host, port, database_name, username, encrypted_password,
 connection_params, status, last_test_at, description, created_by)
VALUES
(9001, 'ds_goai_risk_feature_store', 'GOAI 演示 · 风控特征数据湖', 'HIVE',
 'risk-data-lake.staging.internal', 10000, 'risk_dw', NULL, NULL,
 '{"environment":"staging","credentialMode":"external_secret","containsCustomerData":false}',
 'active', '2026-08-02 09:55:00',
 '跨域特征漂移 Demo 的脱敏交易事实、生产特征和推理结果目录；仅用于 GOAI 复赛演示。',
 'goai-fixture'),
(9002, 'ds_goai_training_history', 'GOAI 演示 · 历史训练样本库', 'S3',
 'training-datasets.staging.internal', 443, 'risk-training-history', NULL, NULL,
 '{"environment":"staging","credentialMode":"external_secret","containsCustomerData":false}',
 'active', '2026-08-02 09:56:00',
 '保存脱敏历史训练样本与回填数据版本；仅用于 GOAI 复赛演示。',
 'goai-fixture'),
(9003, 'ds_goai_prediction_stream', 'GOAI 演示 · 在线推理结果流', 'KAFKA',
 'risk-events.staging.internal', 9092, 'risk-predictions', NULL, NULL,
 '{"environment":"staging","credentialMode":"external_secret","containsCustomerData":false}',
 'active', '2026-08-02 10:05:00',
 '接收风险模型推理结果和业务效果事件；仅用于 GOAI 复赛演示。',
 'goai-fixture')
ON DUPLICATE KEY UPDATE
name = VALUES(name), type = VALUES(type), host = VALUES(host), port = VALUES(port),
database_name = VALUES(database_name), username = VALUES(username),
encrypted_password = VALUES(encrypted_password), connection_params = VALUES(connection_params),
status = VALUES(status), last_test_at = VALUES(last_test_at),
description = VALUES(description), created_by = VALUES(created_by);

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
