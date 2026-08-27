-- 补齐 GOAI 复赛演示环境的数据源目录。
-- 本迁移仅登记受控 staging 连接元数据，不保存外部系统账号或密码。
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
name = VALUES(name),
type = VALUES(type),
host = VALUES(host),
port = VALUES(port),
database_name = VALUES(database_name),
username = VALUES(username),
encrypted_password = VALUES(encrypted_password),
connection_params = VALUES(connection_params),
status = VALUES(status),
last_test_at = VALUES(last_test_at),
description = VALUES(description),
created_by = VALUES(created_by);

SELECT
  COUNT(*) = 3 AS datasource_fixture_ready
FROM xnet_dataops_dsm_datasource
WHERE uid IN (
  'ds_goai_risk_feature_store',
  'ds_goai_training_history',
  'ds_goai_prediction_stream'
);
