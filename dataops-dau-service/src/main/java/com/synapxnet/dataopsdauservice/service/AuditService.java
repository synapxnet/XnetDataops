package com.synapxnet.dataopsdauservice.service;

import com.synapxnet.dataopsdauservice.entity.AuditLog;
import com.synapxnet.dataopsdauservice.entity.ComplianceReport;
import com.synapxnet.dataopsdauservice.entity.DataChangeRecord;

import java.util.List;
import java.util.Map;

public interface AuditService {

    // Audit Log operations
    List<AuditLog> listLogs(String module, String action, Long userId);
    AuditLog getLogById(Long id);
    AuditLog createLog(AuditLog auditLog);
    Map<String, Object> getLogStats();

    // Data Change Record operations
    List<DataChangeRecord> listChanges(Long datasourceId, String tableName);
    DataChangeRecord getChangeById(Long id);
    DataChangeRecord recordChange(DataChangeRecord record);

    // Compliance Report operations
    List<ComplianceReport> listReports();
    ComplianceReport getReportById(Long id);
    ComplianceReport generateReport(ComplianceReport report);
    ComplianceReport reviewReport(Long id);
    ComplianceReport archiveReport(Long id);
}
