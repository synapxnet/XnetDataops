package com.synapxnet.dataopsdmsservice.mapper;

import com.synapxnet.dataopsdmsservice.entity.MaskingRule;
import com.synapxnet.dataopsdmsservice.entity.MaskingPolicy;
import com.synapxnet.dataopsdmsservice.entity.MaskingTaskLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MaskingRuleMapper {
    // Rules
    @Select("SELECT * FROM xnet_dataops_dms_masking_rule ORDER BY created_at DESC")
    List<MaskingRule> findAllRules();

    @Select("SELECT * FROM xnet_dataops_dms_masking_rule WHERE id = #{id}")
    MaskingRule findRuleById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dms_masking_rule (uid, name, rule_type, mask_pattern, replacement, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{ruleType}, #{maskPattern}, #{replacement}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRule(MaskingRule rule);

    @Update("UPDATE xnet_dataops_dms_masking_rule SET name=#{name}, rule_type=#{ruleType}, mask_pattern=#{maskPattern}, " +
            "replacement=#{replacement}, description=#{description} WHERE id=#{id}")
    int updateRule(MaskingRule rule);

    @Delete("DELETE FROM xnet_dataops_dms_masking_rule WHERE id = #{id}")
    int deleteRule(@Param("id") Long id);

    // Policies
    @Select("SELECT * FROM xnet_dataops_dms_masking_policy ORDER BY priority ASC, created_at DESC")
    List<MaskingPolicy> findAllPolicies();

    @Select("SELECT * FROM xnet_dataops_dms_masking_policy WHERE datasource_id = #{datasourceId} ORDER BY priority ASC, created_at DESC")
    List<MaskingPolicy> findPoliciesByDatasourceId(@Param("datasourceId") Long datasourceId);

    @Select("SELECT * FROM xnet_dataops_dms_masking_policy WHERE id = #{id}")
    MaskingPolicy findPolicyById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dms_masking_policy (uid, name, datasource_id, table_name, column_name, rule_id, enabled, priority, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{tableName}, #{columnName}, #{ruleId}, #{enabled}, #{priority}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPolicy(MaskingPolicy policy);

    @Update("UPDATE xnet_dataops_dms_masking_policy SET name=#{name}, datasource_id=#{datasourceId}, table_name=#{tableName}, column_name=#{columnName}, " +
            "rule_id=#{ruleId}, enabled=#{enabled}, priority=#{priority}, description=#{description} WHERE id=#{id}")
    int updatePolicy(MaskingPolicy policy);

    @Update("UPDATE xnet_dataops_dms_masking_policy SET enabled=#{enabled} WHERE id=#{id}")
    int togglePolicy(@Param("id") Long id, @Param("enabled") Boolean enabled);

    @Delete("DELETE FROM xnet_dataops_dms_masking_policy WHERE id = #{id}")
    int deletePolicy(@Param("id") Long id);

    // Task Logs
    @Select("SELECT * FROM xnet_dataops_dms_masking_task_log ORDER BY created_at DESC LIMIT 100")
    List<MaskingTaskLog> findRecentLogs();

    @Select("SELECT * FROM xnet_dataops_dms_masking_task_log WHERE policy_id = #{policyId} ORDER BY created_at DESC")
    List<MaskingTaskLog> findLogsByPolicyId(@Param("policyId") Long policyId);
}
