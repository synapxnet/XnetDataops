package com.synapxnet.dataopsdqmservice.service;

import com.synapxnet.dataopsdqmservice.entity.QualityRule;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.entity.QualityAlert;
import java.util.List;

public interface QualityRuleService {
    List<QualityRule> listRules();
    QualityRule getRuleById(Long id);
    QualityRule createRule(QualityRule rule);
    QualityRule updateRule(QualityRule rule);
    void deleteRule(Long id);
    List<QualityReport> listReports(Long ruleId);
    List<QualityAlert> listAlerts(String status);
    void resolveAlert(Long id);
    void acknowledgeAlert(Long id);
}
