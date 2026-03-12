package com.synapxnet.dataopsdapservice.service;

import com.synapxnet.dataopsdapservice.entity.ApiCallLog;
import com.synapxnet.dataopsdapservice.entity.ApiConfig;
import com.synapxnet.dataopsdapservice.entity.ApiKey;

import java.util.List;
import java.util.Map;

public interface ApiConfigService {

    // ApiConfig
    List<ApiConfig> listAllConfigs();
    ApiConfig getConfigById(Long id);
    ApiConfig createConfig(ApiConfig apiConfig);
    ApiConfig updateConfig(ApiConfig apiConfig);
    void deleteConfig(Long id);
    ApiConfig publishConfig(Long id);
    ApiConfig deprecateConfig(Long id);

    // ApiKey
    List<ApiKey> listAllKeys();
    ApiKey createKey(ApiKey apiKey);
    ApiKey revokeKey(Long id);
    void deleteKey(Long id);

    // ApiCallLog
    List<ApiCallLog> listLogs(Long apiConfigId);
    Map<String, Object> getCallStats(Long apiConfigId);
}
