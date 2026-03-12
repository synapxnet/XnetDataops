package com.synapxnet.dataopsdqmservice.mapper;

import com.synapxnet.dataopsdqmservice.entity.QualityRule;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.entity.QualityAlert;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QualityRuleMapper {
    @Select("SELECT * FROM xnet_dataops_dqm_quality_rule ORDER BY created_at DESC")
    List<QualityRule> findAllRules();

    @Select("SELECT * FROM xnet_dataops_dqm_quality_rule WHERE id = #{id}")
    QualityRule findRuleById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dqm_quality_rule (uid, name, datasource_id, table_name, column_name, rule_type, rule_expression, severity, enabled, schedule_cron, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{tableName}, #{columnName}, #{ruleType}, #{ruleExpression}, #{severity}, #{enabled}, #{scheduleCron}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRule(QualityRule rule);

    @Update("UPDATE xnet_dataops_dqm_quality_rule SET name=#{name}, datasource_id=#{datasourceId}, table_name=#{tableName}, column_name=#{columnName}, " +
            "rule_type=#{ruleType}, rule_expression=#{ruleExpression}, severity=#{severity}, enabled=#{enabled}, schedule_cron=#{scheduleCron}, description=#{description} WHERE id=#{id}")
    int updateRule(QualityRule rule);

    @Delete("DELETE FROM xnet_dataops_dqm_quality_rule WHERE id = #{id}")
    int deleteRule(@Param("id") Long id);

    // Reports
    @Select("SELECT * FROM xnet_dataops_dqm_quality_report ORDER BY check_time DESC LIMIT 100")
    List<QualityReport> findRecentReports();

    @Select("SELECT * FROM xnet_dataops_dqm_quality_report WHERE rule_id = #{ruleId} ORDER BY check_time DESC")
    List<QualityReport> findReportsByRuleId(@Param("ruleId") Long ruleId);

    @Insert("INSERT INTO xnet_dataops_dqm_quality_report (uid, rule_id, check_time, status, total_rows, failed_rows, pass_rate, detail_json) " +
            "VALUES (#{uid}, #{ruleId}, #{checkTime}, #{status}, #{totalRows}, #{failedRows}, #{passRate}, #{detailJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(QualityReport report);

    // Alerts
    @Select("SELECT * FROM xnet_dataops_dqm_quality_alert ORDER BY triggered_at DESC LIMIT 100")
    List<QualityAlert> findRecentAlerts();

    @Select("SELECT * FROM xnet_dataops_dqm_quality_alert WHERE status = #{status} ORDER BY triggered_at DESC")
    List<QualityAlert> findAlertsByStatus(@Param("status") String status);

    @Insert("INSERT INTO xnet_dataops_dqm_quality_alert (uid, rule_id, report_id, alert_level, message, status) " +
            "VALUES (#{uid}, #{ruleId}, #{reportId}, #{alertLevel}, #{message}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAlert(QualityAlert alert);

    @Update("UPDATE xnet_dataops_dqm_quality_alert SET status=#{status}, resolved_at=NOW() WHERE id=#{id}")
    int updateAlertStatus(@Param("id") Long id, @Param("status") String status);
}
