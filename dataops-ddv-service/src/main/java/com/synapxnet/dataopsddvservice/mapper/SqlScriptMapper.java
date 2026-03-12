package com.synapxnet.dataopsddvservice.mapper;

import com.synapxnet.dataopsddvservice.entity.SqlScript;
import com.synapxnet.dataopsddvservice.entity.QueryHistory;
import com.synapxnet.dataopsddvservice.entity.SavedQuery;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SqlScriptMapper {
    @Select("SELECT * FROM xnet_dataops_ddv_sql_script ORDER BY updated_at DESC")
    List<SqlScript> findAllScripts();

    @Select("SELECT * FROM xnet_dataops_ddv_sql_script WHERE id = #{id}")
    SqlScript findScriptById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_ddv_sql_script (uid, name, datasource_id, content, script_type, folder_path, status, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{content}, #{scriptType}, #{folderPath}, #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertScript(SqlScript script);

    @Update("UPDATE xnet_dataops_ddv_sql_script SET name=#{name}, datasource_id=#{datasourceId}, content=#{content}, script_type=#{scriptType}, folder_path=#{folderPath}, status=#{status} WHERE id=#{id}")
    int updateScript(SqlScript script);

    @Delete("DELETE FROM xnet_dataops_ddv_sql_script WHERE id = #{id}")
    int deleteScript(@Param("id") Long id);

    // QueryHistory
    @Select("SELECT * FROM xnet_dataops_ddv_query_history ORDER BY executed_at DESC LIMIT 100")
    List<QueryHistory> findRecentHistory();

    @Select("SELECT * FROM xnet_dataops_ddv_query_history WHERE datasource_id = #{datasourceId} ORDER BY executed_at DESC LIMIT 100")
    List<QueryHistory> findHistoryByDatasource(@Param("datasourceId") Long datasourceId);

    @Insert("INSERT INTO xnet_dataops_ddv_query_history (uid, datasource_id, sql_content, execute_status, rows_affected, duration_ms, error_msg, executed_by) " +
            "VALUES (#{uid}, #{datasourceId}, #{sqlContent}, #{executeStatus}, #{rowsAffected}, #{durationMs}, #{errorMsg}, #{executedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertHistory(QueryHistory history);

    // SavedQuery
    @Select("SELECT * FROM xnet_dataops_ddv_saved_query ORDER BY updated_at DESC")
    List<SavedQuery> findAllSaved();

    @Select("SELECT * FROM xnet_dataops_ddv_saved_query WHERE id = #{id}")
    SavedQuery findSavedById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_ddv_saved_query (uid, name, datasource_id, sql_content, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{sqlContent}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSaved(SavedQuery saved);

    @Update("UPDATE xnet_dataops_ddv_saved_query SET name=#{name}, datasource_id=#{datasourceId}, sql_content=#{sqlContent}, description=#{description} WHERE id=#{id}")
    int updateSaved(SavedQuery saved);

    @Delete("DELETE FROM xnet_dataops_ddv_saved_query WHERE id = #{id}")
    int deleteSaved(@Param("id") Long id);
}
