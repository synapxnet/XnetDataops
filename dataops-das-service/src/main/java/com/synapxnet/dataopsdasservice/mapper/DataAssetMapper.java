package com.synapxnet.dataopsdasservice.mapper;

import com.synapxnet.dataopsdasservice.entity.AssetAccessRecord;
import com.synapxnet.dataopsdasservice.entity.AssetClassification;
import com.synapxnet.dataopsdasservice.entity.DataAsset;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DataAssetMapper {

    // ==================== DataAsset ====================

    @Select("SELECT * FROM xnet_dataops_das_asset ORDER BY created_at DESC")
    List<DataAsset> findAllAssets();

    @Select("SELECT * FROM xnet_dataops_das_asset WHERE domain = #{domain} ORDER BY created_at DESC")
    List<DataAsset> findAssetsByDomain(@Param("domain") String domain);

    @Select("SELECT * FROM xnet_dataops_das_asset WHERE category = #{category} ORDER BY created_at DESC")
    List<DataAsset> findAssetsByCategory(@Param("category") String category);

    @Select("SELECT * FROM xnet_dataops_das_asset WHERE domain = #{domain} AND category = #{category} ORDER BY created_at DESC")
    List<DataAsset> findAssetsByDomainAndCategory(@Param("domain") String domain, @Param("category") String category);

    @Select("SELECT * FROM xnet_dataops_das_asset WHERE id = #{id}")
    DataAsset findAssetById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_das_asset (uid, name, datasource_id, table_name, asset_type, category, domain, owner, description, " +
            "status, access_level, row_count, data_size_bytes, quality_score, last_profiled_at, created_by) " +
            "VALUES (#{uid}, #{name}, #{datasourceId}, #{tableName}, #{assetType}, #{category}, #{domain}, #{owner}, #{description}, " +
            "#{status}, #{accessLevel}, #{rowCount}, #{dataSizeBytes}, #{qualityScore}, #{lastProfiledAt}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAsset(DataAsset asset);

    @Update("UPDATE xnet_dataops_das_asset SET name=#{name}, datasource_id=#{datasourceId}, table_name=#{tableName}, " +
            "asset_type=#{assetType}, category=#{category}, domain=#{domain}, owner=#{owner}, description=#{description}, " +
            "status=#{status}, access_level=#{accessLevel}, row_count=#{rowCount}, data_size_bytes=#{dataSizeBytes}, " +
            "quality_score=#{qualityScore}, last_profiled_at=#{lastProfiledAt} WHERE id=#{id}")
    int updateAsset(DataAsset asset);

    @Delete("DELETE FROM xnet_dataops_das_asset WHERE id = #{id}")
    int deleteAssetById(@Param("id") Long id);

    @Select("SELECT domain, COUNT(*) AS cnt FROM xnet_dataops_das_asset GROUP BY domain")
    List<Map<String, Object>> countByDomain();

    @Select("SELECT category, COUNT(*) AS cnt FROM xnet_dataops_das_asset GROUP BY category")
    List<Map<String, Object>> countByCategory();

    @Select("SELECT access_level AS accessLevel, COUNT(*) AS cnt FROM xnet_dataops_das_asset GROUP BY access_level")
    List<Map<String, Object>> countByAccessLevel();

    // ==================== AssetClassification ====================

    @Select("SELECT * FROM xnet_dataops_das_classification ORDER BY sort_order ASC, created_at ASC")
    List<AssetClassification> findAllClassifications();

    @Select("SELECT * FROM xnet_dataops_das_classification WHERE id = #{id}")
    AssetClassification findClassificationById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_das_classification (name, level, parent_id, description, sort_order) " +
            "VALUES (#{name}, #{level}, #{parentId}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertClassification(AssetClassification classification);

    @Update("UPDATE xnet_dataops_das_classification SET name=#{name}, level=#{level}, parent_id=#{parentId}, " +
            "description=#{description}, sort_order=#{sortOrder} WHERE id=#{id}")
    int updateClassification(AssetClassification classification);

    @Delete("DELETE FROM xnet_dataops_das_classification WHERE id = #{id}")
    int deleteClassificationById(@Param("id") Long id);

    // ==================== AssetAccessRecord ====================

    @Select("SELECT * FROM xnet_dataops_das_access_record WHERE asset_id = #{assetId} ORDER BY access_time DESC")
    List<AssetAccessRecord> findAccessRecordsByAssetId(@Param("assetId") Long assetId);

    @Insert("INSERT INTO xnet_dataops_das_access_record (uid, asset_id, user_id, access_type, access_time, ip_address, detail) " +
            "VALUES (#{uid}, #{assetId}, #{userId}, #{accessType}, #{accessTime}, #{ipAddress}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAccessRecord(AssetAccessRecord record);
}
