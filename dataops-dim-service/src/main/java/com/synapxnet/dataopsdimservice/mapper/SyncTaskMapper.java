package com.synapxnet.dataopsdimservice.mapper;

import com.synapxnet.dataopsdimservice.entity.SyncTask;
import com.synapxnet.dataopsdimservice.entity.FieldMapping;
import com.synapxnet.dataopsdimservice.entity.SyncLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SyncTaskMapper {

    @Select("SELECT * FROM xnet_dataops_dim_sync_task ORDER BY created_at DESC")
    List<SyncTask> findAll();

    @Select("SELECT * FROM xnet_dataops_dim_sync_task WHERE id = #{id}")
    SyncTask findById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dim_sync_task (uid, name, source_ds_id, target_ds_id, source_table, target_table, sync_mode, incremental_field, schedule_cron, status, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{sourceDsId}, #{targetDsId}, #{sourceTable}, #{targetTable}, #{syncMode}, #{incrementalField}, #{scheduleCron}, #{status}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SyncTask task);

    @Update("UPDATE xnet_dataops_dim_sync_task SET name=#{name}, source_ds_id=#{sourceDsId}, target_ds_id=#{targetDsId}, " +
            "source_table=#{sourceTable}, target_table=#{targetTable}, sync_mode=#{syncMode}, incremental_field=#{incrementalField}, " +
            "schedule_cron=#{scheduleCron}, description=#{description} WHERE id=#{id}")
    int update(SyncTask task);

    @Update("UPDATE xnet_dataops_dim_sync_task SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_dim_sync_task WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    // FieldMapping
    @Select("SELECT * FROM xnet_dataops_dim_field_mapping WHERE task_id = #{taskId} ORDER BY sort_order")
    List<FieldMapping> findMappingsByTaskId(@Param("taskId") Long taskId);

    @Insert("INSERT INTO xnet_dataops_dim_field_mapping (task_id, source_field, target_field, transform_expression, sort_order) " +
            "VALUES (#{taskId}, #{sourceField}, #{targetField}, #{transformExpression}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMapping(FieldMapping mapping);

    @Delete("DELETE FROM xnet_dataops_dim_field_mapping WHERE task_id = #{taskId}")
    int deleteMappingsByTaskId(@Param("taskId") Long taskId);

    // SyncLog
    @Select("SELECT * FROM xnet_dataops_dim_sync_log WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<SyncLog> findLogsByTaskId(@Param("taskId") Long taskId);

    @Insert("INSERT INTO xnet_dataops_dim_sync_log (uid, task_id, start_time, status) VALUES (#{uid}, #{taskId}, NOW(), #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLog(SyncLog log);

    @Update("UPDATE xnet_dataops_dim_sync_log SET end_time=NOW(), status=#{status}, rows_read=#{rowsRead}, rows_written=#{rowsWritten}, error_msg=#{errorMsg} WHERE id=#{id}")
    int updateLog(SyncLog log);
}
