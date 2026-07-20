package com.synapxnet.dataopsdmsservice.service.impl;

import com.synapxnet.dataopsdmsservice.entity.MaskingRule;
import com.synapxnet.dataopsdmsservice.entity.MaskingPolicy;
import com.synapxnet.dataopsdmsservice.entity.MaskingTaskLog;
import com.synapxnet.dataopsdmsservice.mapper.MaskingRuleMapper;
import com.synapxnet.dataopsdmsservice.service.MaskingService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class MaskingServiceImpl implements MaskingService {
    private final MaskingRuleMapper mapper;

    public MaskingServiceImpl(MaskingRuleMapper mapper) { this.mapper = mapper; }

    // Rules
    @Override public List<MaskingRule> listRules() { return mapper.findAllRules(); }

    @Override public MaskingRule getRuleById(Long id) {
        MaskingRule rule = mapper.findRuleById(id);
        if (rule == null) throw new IllegalArgumentException("MaskingRule not found: " + id);
        return rule;
    }

    @Override public MaskingRule createRule(MaskingRule rule) {
        rule.setUid(UUID.randomUUID().toString());
        mapper.insertRule(rule);
        return rule;
    }

    @Override public MaskingRule updateRule(MaskingRule rule) {
        mapper.updateRule(rule);
        return mapper.findRuleById(rule.getId());
    }

    @Override public void deleteRule(Long id) { mapper.deleteRule(id); }

    // Policies
    @Override public List<MaskingPolicy> listPolicies(Long datasourceId) {
        if (datasourceId != null) return mapper.findPoliciesByDatasourceId(datasourceId);
        return mapper.findAllPolicies();
    }

    @Override public MaskingPolicy getPolicyById(Long id) {
        MaskingPolicy policy = mapper.findPolicyById(id);
        if (policy == null) throw new IllegalArgumentException("MaskingPolicy not found: " + id);
        return policy;
    }

    @Override public MaskingPolicy createPolicy(MaskingPolicy policy) {
        policy.setUid(UUID.randomUUID().toString());
        if (policy.getEnabled() == null) policy.setEnabled(true);
        if (policy.getPriority() == null) policy.setPriority(0);
        mapper.insertPolicy(policy);
        return policy;
    }

    @Override public MaskingPolicy updatePolicy(MaskingPolicy policy) {
        mapper.updatePolicy(policy);
        return mapper.findPolicyById(policy.getId());
    }

    @Override public void deletePolicy(Long id) { mapper.deletePolicy(id); }

    @Override public MaskingPolicy togglePolicy(Long id) {
        MaskingPolicy policy = getPolicyById(id);
        Boolean newEnabled = !Boolean.TRUE.equals(policy.getEnabled());
        mapper.togglePolicy(id, newEnabled);
        return mapper.findPolicyById(id);
    }

    // Task Logs
    @Override public List<MaskingTaskLog> listLogs(Long policyId) {
        if (policyId != null) return mapper.findLogsByPolicyId(policyId);
        return mapper.findRecentLogs();
    }
}
