package com.synapxnet.dataopsdapservice.mapper;

import com.synapxnet.dataopsdapservice.entity.ApiCallLog;
import com.synapxnet.dataopsdapservice.entity.ApiConfig;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ApiConfigMapper {

    // ==================== ApiConfig CRUD ====================

    @Select("SELECT * FROM xnet_dataops_dap_api_config ORDER BY created_at DESC")
    List<ApiConfig> findAllConfigs();

    @Select("SELECT * FROM xnet_dataops_dap_api_config WHERE id = #{id}")
    ApiConfig findConfigById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dap_api_config (uid, name, path, method, datasource_id, sql_content, param_config, description, status, rate_limit, cache_ttl, created_by) " +
            "VALUES (#{uid}, #{name}, #{path}, #{method}, #{datasourceId}, #{sqlContent}, #{paramConfig}, #{description}, #{status}, #{rateLimit}, #{cacheTtl}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertConfig(ApiConfig apiConfig);

    @Update("UPDATE xnet_dataops_dap_api_config SET name=#{name}, path=#{path}, method=#{method}, datasource_id=#{datasourceId}, " +
            "sql_content=#{sqlContent}, param_config=#{paramConfig}, description=#{description}, " +
            "rate_limit=#{rateLimit}, cache_ttl=#{cacheTtl}, updated_at=NOW() WHERE id=#{id}")
    int updateConfig(ApiConfig apiConfig);

    @Update("UPDATE xnet_dataops_dap_api_config SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateConfigStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_dap_api_config WHERE id = #{id}")
    int deleteConfigById(@Param("id") Long id);

    // ==================== ApiKey CRUD ====================

    @Select("SELECT * FROM xnet_dataops_dap_api_key ORDER BY created_at DESC")
    List<ApiKey> findAllKeys();

    @Select("SELECT * FROM xnet_dataops_dap_api_key WHERE id = #{id}")
    ApiKey findKeyById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dap_api_key (uid, app_name, api_key, secret_key, status, permissions, expire_at, created_by) " +
            "VALUES (#{uid}, #{appName}, #{apiKey}, #{secretKey}, #{status}, #{permissions}, #{expireAt}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertKey(ApiKey apiKey);

    @Update("UPDATE xnet_dataops_dap_api_key SET status=#{status} WHERE id=#{id}")
    int updateKeyStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_dap_api_key WHERE id = #{id}")
    int deleteKeyById(@Param("id") Long id);

    // ==================== ApiCallLog CRUD ====================

    @Select("SELECT * FROM xnet_dataops_dap_api_call_log ORDER BY called_at DESC")
    List<ApiCallLog> findAllLogs();

    @Select("SELECT * FROM xnet_dataops_dap_api_call_log WHERE api_config_id = #{apiConfigId} ORDER BY called_at DESC")
    List<ApiCallLog> findLogsByApiConfigId(@Param("apiConfigId") Long apiConfigId);

    @Insert("INSERT INTO xnet_dataops_dap_api_call_log (api_config_id, api_key_id, request_params, response_status, response_time, ip_address) " +
            "VALUES (#{apiConfigId}, #{apiKeyId}, #{requestParams}, #{responseStatus}, #{responseTime}, #{ipAddress})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLog(ApiCallLog apiCallLog);

    @Select("SELECT COUNT(*) AS total_calls, " +
            "AVG(response_time) AS avg_response_time, " +
            "SUM(CASE WHEN response_status = 200 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN response_status != 200 THEN 1 ELSE 0 END) AS error_count " +
            "FROM xnet_dataops_dap_api_call_log")
    Map<String, Object> getCallStats();

    @Select("SELECT COUNT(*) AS total_calls, " +
            "AVG(response_time) AS avg_response_time, " +
            "SUM(CASE WHEN response_status = 200 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN response_status != 200 THEN 1 ELSE 0 END) AS error_count " +
            "FROM xnet_dataops_dap_api_call_log WHERE api_config_id = #{apiConfigId}")
    Map<String, Object> getCallStatsByApiConfigId(@Param("apiConfigId") Long apiConfigId);
}
