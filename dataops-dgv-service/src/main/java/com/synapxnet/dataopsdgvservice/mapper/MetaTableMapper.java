package com.synapxnet.dataopsdgvservice.mapper;

import com.synapxnet.dataopsdgvservice.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MetaTableMapper {
    @Select("SELECT * FROM xnet_dataops_dgv_meta_table ORDER BY updated_at DESC")
    List<MetaTable> findAllTables();

    @Select("SELECT * FROM xnet_dataops_dgv_meta_table WHERE id = #{id}")
    MetaTable findTableById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_dataops_dgv_meta_table WHERE datasource_id = #{datasourceId} ORDER BY table_name")
    List<MetaTable> findTablesByDatasourceId(@Param("datasourceId") Long datasourceId);

    @Insert("INSERT INTO xnet_dataops_dgv_meta_table (uid, datasource_id, schema_name, table_name, table_type, row_count, data_size_bytes, description, owner) " +
            "VALUES (#{uid}, #{datasourceId}, #{schemaName}, #{tableName}, #{tableType}, #{rowCount}, #{dataSizeBytes}, #{description}, #{owner})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTable(MetaTable table);

    @Update("UPDATE xnet_dataops_dgv_meta_table SET schema_name=#{schemaName}, table_name=#{tableName}, table_type=#{tableType}, " +
            "row_count=#{rowCount}, data_size_bytes=#{dataSizeBytes}, description=#{description}, owner=#{owner}, last_sync_at=NOW() WHERE id=#{id}")
    int updateTable(MetaTable table);

    @Delete("DELETE FROM xnet_dataops_dgv_meta_table WHERE id = #{id}")
    int deleteTable(@Param("id") Long id);

    // Columns
    @Select("SELECT * FROM xnet_dataops_dgv_meta_column WHERE meta_table_id = #{metaTableId} ORDER BY sort_order")
    List<MetaColumn> findColumnsByTableId(@Param("metaTableId") Long metaTableId);

    @Insert("INSERT INTO xnet_dataops_dgv_meta_column (meta_table_id, column_name, column_type, is_nullable, is_primary_key, default_value, description, sort_order) " +
            "VALUES (#{metaTableId}, #{columnName}, #{columnType}, #{isNullable}, #{isPrimaryKey}, #{defaultValue}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertColumn(MetaColumn column);

    @Delete("DELETE FROM xnet_dataops_dgv_meta_column WHERE meta_table_id = #{metaTableId}")
    int deleteColumnsByTableId(@Param("metaTableId") Long metaTableId);

    // Lineage
    @Select("SELECT * FROM xnet_dataops_dgv_data_lineage ORDER BY created_at DESC")
    List<DataLineage> findAllLineage();

    @Select("SELECT * FROM xnet_dataops_dgv_data_lineage WHERE source_table_id = #{tableId} OR target_table_id = #{tableId}")
    List<DataLineage> findLineageByTableId(@Param("tableId") Long tableId);

    @Insert("INSERT INTO xnet_dataops_dgv_data_lineage (uid, source_table_id, target_table_id, transform_type, relationship_desc, workflow_id) " +
            "VALUES (#{uid}, #{sourceTableId}, #{targetTableId}, #{transformType}, #{relationshipDesc}, #{workflowId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLineage(DataLineage lineage);

    @Delete("DELETE FROM xnet_dataops_dgv_data_lineage WHERE id = #{id}")
    int deleteLineage(@Param("id") Long id);

    // Tags
    @Select("SELECT * FROM xnet_dataops_dgv_data_tag ORDER BY created_at DESC")
    List<DataTag> findAllTags();

    @Insert("INSERT INTO xnet_dataops_dgv_data_tag (name, tag_type, color, description) VALUES (#{name}, #{tagType}, #{color}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTag(DataTag tag);

    @Update("UPDATE xnet_dataops_dgv_data_tag SET name=#{name}, tag_type=#{tagType}, color=#{color}, description=#{description} WHERE id=#{id}")
    int updateTag(DataTag tag);

    @Delete("DELETE FROM xnet_dataops_dgv_data_tag WHERE id = #{id}")
    int deleteTag(@Param("id") Long id);

    // Table-Tag
    @Select("SELECT t.* FROM xnet_dataops_dgv_data_tag t JOIN xnet_dataops_dgv_table_tag tt ON t.id = tt.tag_id WHERE tt.meta_table_id = #{metaTableId}")
    List<DataTag> findTagsByTableId(@Param("metaTableId") Long metaTableId);

    @Insert("INSERT INTO xnet_dataops_dgv_table_tag (meta_table_id, tag_id) VALUES (#{metaTableId}, #{tagId})")
    int insertTableTag(@Param("metaTableId") Long metaTableId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM xnet_dataops_dgv_table_tag WHERE meta_table_id = #{metaTableId} AND tag_id = #{tagId}")
    int deleteTableTag(@Param("metaTableId") Long metaTableId, @Param("tagId") Long tagId);
}
