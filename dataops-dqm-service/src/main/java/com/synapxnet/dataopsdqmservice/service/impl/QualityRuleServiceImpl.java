package com.synapxnet.dataopsdqmservice.service.impl;

import com.synapxnet.dataopsdqmservice.entity.QualityRule;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.entity.QualityAlert;
import com.synapxnet.dataopsdqmservice.mapper.QualityRuleMapper;
import com.synapxnet.dataopsdqmservice.service.QualityRuleService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class QualityRuleServiceImpl implements QualityRuleService {
    private final QualityRuleMapper mapper;

    public QualityRuleServiceImpl(QualityRuleMapper mapper) { this.mapper = mapper; }

    @Override public List<QualityRule> listRules() { return mapper.findAllRules(); }

    @Override public QualityRule getRuleById(Long id) {
        QualityRule rule = mapper.findRuleById(id);
        if (rule == null) throw new IllegalArgumentException("QualityRule not found: " + id);
        return rule;
    }

    @Override public QualityRule createRule(QualityRule rule) {
        rule.setUid(UUID.randomUUID().toString());
        if (rule.getSeverity() == null) rule.setSeverity("warning");
        if (rule.getEnabled() == null) rule.setEnabled(true);
        mapper.insertRule(rule);
        return rule;
    }

    @Override public QualityRule updateRule(QualityRule rule) {
        mapper.updateRule(rule);
        return mapper.findRuleById(rule.getId());
    }

    @Override public void deleteRule(Long id) { mapper.deleteRule(id); }

    @Override public List<QualityReport> listReports(Long ruleId) {
        if (ruleId != null) return mapper.findReportsByRuleId(ruleId);
        return mapper.findRecentReports();
    }

    @Override public List<QualityAlert> listAlerts(String status) {
        if (status != null && !status.isEmpty()) return mapper.findAlertsByStatus(status);
        return mapper.findRecentAlerts();
    }

    @Override public void resolveAlert(Long id) { mapper.updateAlertStatus(id, "resolved"); }
    @Override public void acknowledgeAlert(Long id) { mapper.updateAlertStatus(id, "acknowledged"); }
}
