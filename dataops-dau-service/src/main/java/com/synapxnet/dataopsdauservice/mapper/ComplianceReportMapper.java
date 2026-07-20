package com.synapxnet.dataopsdauservice.mapper;

import com.synapxnet.dataopsdauservice.entity.ComplianceReport;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ComplianceReportMapper {

    @Select("SELECT * FROM xnet_dataops_dau_compliance_report ORDER BY created_at DESC")
    List<ComplianceReport> findAll();

    @Select("SELECT * FROM xnet_dataops_dau_compliance_report WHERE id = #{id}")
    ComplianceReport findById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dau_compliance_report (uid, name, report_type, period_start, period_end, total_events, risk_events, status, generated_by, created_at) " +
            "VALUES (#{uid}, #{name}, #{reportType}, #{periodStart}, #{periodEnd}, #{totalEvents}, #{riskEvents}, #{status}, #{generatedBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ComplianceReport report);

    @Update("UPDATE xnet_dataops_dau_compliance_report SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_dau_compliance_report WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
