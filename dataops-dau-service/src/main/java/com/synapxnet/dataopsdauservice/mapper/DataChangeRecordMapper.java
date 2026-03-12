package com.synapxnet.dataopsdauservice.mapper;

import com.synapxnet.dataopsdauservice.entity.DataChangeRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataChangeRecordMapper {

    @Select("SELECT * FROM xnet_dataops_dau_data_change_record ORDER BY changed_at DESC")
    List<DataChangeRecord> findAll();

    @Select("SELECT * FROM xnet_dataops_dau_data_change_record WHERE id = #{id}")
    DataChangeRecord findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_dataops_dau_data_change_record WHERE datasource_id = #{datasourceId} ORDER BY changed_at DESC")
    List<DataChangeRecord> findByDatasourceId(@Param("datasourceId") Long datasourceId);

    @Select("SELECT * FROM xnet_dataops_dau_data_change_record WHERE table_name = #{tableName} ORDER BY changed_at DESC")
    List<DataChangeRecord> findByTableName(@Param("tableName") String tableName);

    @Select("SELECT * FROM xnet_dataops_dau_data_change_record WHERE datasource_id = #{datasourceId} AND table_name = #{tableName} ORDER BY changed_at DESC")
    List<DataChangeRecord> findByDatasourceIdAndTableName(@Param("datasourceId") Long datasourceId, @Param("tableName") String tableName);

    @Insert("INSERT INTO xnet_dataops_dau_data_change_record (uid, datasource_id, table_name, change_type, affected_rows, change_sql, changed_by, changed_at) " +
            "VALUES (#{uid}, #{datasourceId}, #{tableName}, #{changeType}, #{affectedRows}, #{changeSql}, #{changedBy}, #{changedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataChangeRecord record);

    @Delete("DELETE FROM xnet_dataops_dau_data_change_record WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
