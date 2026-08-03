-- GOAI 1.0.0 迁移校验：四行均应返回 1，fixture 行数由后续脚本校验。
SELECT COUNT(*) = 1 AS dgv_snapshot_table_ready
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xnet_dataops_dgv_schema_snapshot';
SELECT COUNT(*) = 1 AS dqm_contract_table_ready
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xnet_dataops_dqm_contract_check';
SELECT COUNT(*) = 1 AS dau_receipt_table_ready
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xnet_dataops_agent_audit_receipt';
SELECT COUNT(*) = 3 AS dob_link_columns_ready
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'xnet_dataops_dob_monitor_event'
  AND COLUMN_NAME IN ('incident_id', 'trace_id', 'evidence_id');
