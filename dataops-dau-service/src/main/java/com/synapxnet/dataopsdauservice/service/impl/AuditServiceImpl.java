package com.synapxnet.dataopsdauservice.service.impl;

import com.synapxnet.dataopsdauservice.entity.AuditLog;
import com.synapxnet.dataopsdauservice.entity.ComplianceReport;
import com.synapxnet.dataopsdauservice.entity.DataChangeRecord;
import com.synapxnet.dataopsdauservice.mapper.AuditLogMapper;
import com.synapxnet.dataopsdauservice.mapper.ComplianceReportMapper;
import com.synapxnet.dataopsdauservice.mapper.DataChangeRecordMapper;
import com.synapxnet.dataopsdauservice.service.AuditService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;
    private final DataChangeRecordMapper dataChangeRecordMapper;
    private final ComplianceReportMapper complianceReportMapper;

    public AuditServiceImpl(AuditLogMapper auditLogMapper,
                            DataChangeRecordMapper dataChangeRecordMapper,
                            ComplianceReportMapper complianceReportMapper) {
        this.auditLogMapper = auditLogMapper;
        this.dataChangeRecordMapper = dataChangeRecordMapper;
        this.complianceReportMapper = complianceReportMapper;
    }

    // ==================== Audit Log ====================

    @Override
    public List<AuditLog> listLogs(String module, String action, Long userId) {
        boolean hasModule = module != null && !module.isEmpty();
        boolean hasAction = action != null && !action.isEmpty();
        boolean hasUserId = userId != null;

        if (hasModule && hasAction && hasUserId) {
            return auditLogMapper.findByModuleAndActionAndUserId(module, action, userId);
        } else if (hasModule && hasAction) {
            return auditLogMapper.findByModuleAndAction(module, action);
        } else if (hasModule && hasUserId) {
            return auditLogMapper.findByModuleAndUserId(module, userId);
        } else if (hasAction && hasUserId) {
            return auditLogMapper.findByActionAndUserId(action, userId);
        } else if (hasModule) {
            return auditLogMapper.findByModule(module);
        } else if (hasAction) {
            return auditLogMapper.findByAction(action);
        } else if (hasUserId) {
            return auditLogMapper.findByUserId(userId);
        }
        return auditLogMapper.findAll();
    }

    @Override
    public AuditLog getLogById(Long id) {
        AuditLog log = auditLogMapper.findById(id);
        if (log == null) {
            throw new IllegalArgumentException("AuditLog not found: " + id);
        }
        return log;
    }

    @Override
    public AuditLog createLog(AuditLog auditLog) {
        auditLog.setUid(UUID.randomUUID().toString());
        if (auditLog.getOperateAt() == null) {
            auditLog.setOperateAt(LocalDateTime.now());
        }
        auditLogMapper.insert(auditLog);
        return auditLog;
    }

    @Override
    public Map<String, Object> getLogStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", auditLogMapper.countTotal());
        stats.put("byModule", auditLogMapper.countByModule());
        stats.put("byAction", auditLogMapper.countByAction());
        return stats;
    }

    // ==================== Data Change Record ====================

    @Override
    public List<DataChangeRecord> listChanges(Long datasourceId, String tableName) {
        boolean hasDatasourceId = datasourceId != null;
        boolean hasTableName = tableName != null && !tableName.isEmpty();

        if (hasDatasourceId && hasTableName) {
            return dataChangeRecordMapper.findByDatasourceIdAndTableName(datasourceId, tableName);
        } else if (hasDatasourceId) {
            return dataChangeRecordMapper.findByDatasourceId(datasourceId);
        } else if (hasTableName) {
            return dataChangeRecordMapper.findByTableName(tableName);
        }
        return dataChangeRecordMapper.findAll();
    }

    @Override
    public DataChangeRecord getChangeById(Long id) {
        DataChangeRecord record = dataChangeRecordMapper.findById(id);
        if (record == null) {
            throw new IllegalArgumentException("DataChangeRecord not found: " + id);
        }
        return record;
    }

    @Override
    public DataChangeRecord recordChange(DataChangeRecord record) {
        record.setUid(UUID.randomUUID().toString());
        if (record.getChangedAt() == null) {
            record.setChangedAt(LocalDateTime.now());
        }
        dataChangeRecordMapper.insert(record);
        return record;
    }

    // ==================== Compliance Report ====================

    @Override
    public List<ComplianceReport> listReports() {
        return complianceReportMapper.findAll();
    }

    @Override
    public ComplianceReport getReportById(Long id) {
        ComplianceReport report = complianceReportMapper.findById(id);
        if (report == null) {
            throw new IllegalArgumentException("ComplianceReport not found: " + id);
        }
        return report;
    }

    @Override
    public ComplianceReport generateReport(ComplianceReport report) {
        report.setUid(UUID.randomUUID().toString());
        if (report.getStatus() == null) {
            report.setStatus("generated");
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        complianceReportMapper.insert(report);
        return report;
    }

    @Override
    public ComplianceReport reviewReport(Long id) {
        ComplianceReport report = getReportById(id);
        complianceReportMapper.updateStatus(id, "reviewed");
        report.setStatus("reviewed");
        return report;
    }

    @Override
    public ComplianceReport archiveReport(Long id) {
        ComplianceReport report = getReportById(id);
        complianceReportMapper.updateStatus(id, "archived");
        report.setStatus("archived");
        return report;
    }
}
