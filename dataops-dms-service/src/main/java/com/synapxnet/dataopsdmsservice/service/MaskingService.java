package com.synapxnet.dataopsdmsservice.service;

import com.synapxnet.dataopsdmsservice.entity.MaskingRule;
import com.synapxnet.dataopsdmsservice.entity.MaskingPolicy;
import com.synapxnet.dataopsdmsservice.entity.MaskingTaskLog;
import java.util.List;

public interface MaskingService {
    // Rules
    List<MaskingRule> listRules();
    MaskingRule getRuleById(Long id);
    MaskingRule createRule(MaskingRule rule);
    MaskingRule updateRule(MaskingRule rule);
    void deleteRule(Long id);

    // Policies
    List<MaskingPolicy> listPolicies(Long datasourceId);
    MaskingPolicy getPolicyById(Long id);
    MaskingPolicy createPolicy(MaskingPolicy policy);
    MaskingPolicy updatePolicy(MaskingPolicy policy);
    void deletePolicy(Long id);
    MaskingPolicy togglePolicy(Long id);

    // Task Logs
    List<MaskingTaskLog> listLogs(Long policyId);
}
