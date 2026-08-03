-- GOAI 1.0.0 回滚说明：先导出审计与证据，再在停机窗口执行。
ALTER TABLE `xnet_dataops_dob_monitor_event` DROP INDEX `idx_dob_incident_trace`;
ALTER TABLE `xnet_dataops_dob_monitor_event`
    DROP COLUMN `evidence_id`,
    DROP COLUMN `trace_id`,
    DROP COLUMN `incident_id`;
DROP TABLE IF EXISTS `xnet_dataops_agent_audit_receipt`;
DROP TABLE IF EXISTS `xnet_dataops_dqm_contract_check`;
DROP TABLE IF EXISTS `xnet_dataops_dgv_schema_snapshot`;
