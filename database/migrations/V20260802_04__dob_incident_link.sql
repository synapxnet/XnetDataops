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
