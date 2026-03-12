package com.synapxnet.dataopsdobservice.mapper;

import com.synapxnet.dataopsdobservice.entity.DataMonitor;
import com.synapxnet.dataopsdobservice.entity.MonitorEvent;
import com.synapxnet.dataopsdobservice.entity.DataSla;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface DataMonitorMapper {

    // ===== DataMonitor CRUD =====
    @Select("SELECT * FROM xnet_dataops_dob_data_monitor ORDER BY created_at DESC")
    List<DataMonitor> findAllMonitors();

    @Select("SELECT * FROM xnet_dataops_dob_data_monitor WHERE id = #{id}")
    DataMonitor findMonitorById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dob_data_monitor (uid, name, datasource_id, table_name, monitor_type, check_expression, threshold_value, alert_level, enabled, schedule_cron, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{tableName}, #{monitorType}, #{checkExpression}, #{thresholdValue}, #{alertLevel}, #{enabled}, #{scheduleCron}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMonitor(DataMonitor monitor);

    @Update("UPDATE xnet_dataops_dob_data_monitor SET name=#{name}, datasource_id=#{datasourceId}, table_name=#{tableName}, monitor_type=#{monitorType}, " +
            "check_expression=#{checkExpression}, threshold_value=#{thresholdValue}, alert_level=#{alertLevel}, enabled=#{enabled}, schedule_cron=#{scheduleCron}, description=#{description} WHERE id=#{id}")
    int updateMonitor(DataMonitor monitor);

    @Delete("DELETE FROM xnet_dataops_dob_data_monitor WHERE id = #{id}")
    int deleteMonitor(@Param("id") Long id);

    @Update("UPDATE xnet_dataops_dob_data_monitor SET enabled=#{enabled} WHERE id=#{id}")
    int toggleMonitor(@Param("id") Long id, @Param("enabled") Boolean enabled);

    // ===== MonitorEvent =====
    @Select("SELECT * FROM xnet_dataops_dob_monitor_event ORDER BY detected_at DESC LIMIT 100")
    List<MonitorEvent> findRecentEvents();

    @Select("SELECT * FROM xnet_dataops_dob_monitor_event WHERE monitor_id = #{monitorId} ORDER BY detected_at DESC")
    List<MonitorEvent> findEventsByMonitorId(@Param("monitorId") Long monitorId);

    @Select("SELECT * FROM xnet_dataops_dob_monitor_event WHERE status = #{status} ORDER BY detected_at DESC")
    List<MonitorEvent> findEventsByStatus(@Param("status") String status);

    @Select("SELECT * FROM xnet_dataops_dob_monitor_event WHERE monitor_id = #{monitorId} AND status = #{status} ORDER BY detected_at DESC")
    List<MonitorEvent> findEventsByMonitorIdAndStatus(@Param("monitorId") Long monitorId, @Param("status") String status);

    @Update("UPDATE xnet_dataops_dob_monitor_event SET status=#{status} WHERE id=#{id}")
    int updateEventStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE xnet_dataops_dob_monitor_event SET status='resolved', resolved_at=NOW() WHERE id=#{id}")
    int resolveEvent(@Param("id") Long id);

    // ===== DataSla =====
    @Select("SELECT * FROM xnet_dataops_dob_data_sla ORDER BY created_at DESC")
    List<DataSla> findAllSlas();

    @Insert("INSERT INTO xnet_dataops_dob_data_sla (uid, name, pipeline_name, expected_completion_time, actual_completion_time, sla_status, date) " +
            "VALUES (#{uid}, #{name}, #{pipelineName}, #{expectedCompletionTime}, #{actualCompletionTime}, #{slaStatus}, #{date})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSla(DataSla sla);

    @Select("SELECT sla_status, COUNT(*) as count FROM xnet_dataops_dob_data_sla GROUP BY sla_status")
    List<Map<String, Object>> getSlaStats();
}
