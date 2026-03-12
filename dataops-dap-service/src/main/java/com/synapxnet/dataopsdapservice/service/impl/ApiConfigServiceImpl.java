package com.synapxnet.dataopsdapservice.service.impl;

import com.synapxnet.dataopsdapservice.entity.ApiCallLog;
import com.synapxnet.dataopsdapservice.entity.ApiConfig;
import com.synapxnet.dataopsdapservice.entity.ApiKey;
import com.synapxnet.dataopsdapservice.mapper.ApiConfigMapper;
import com.synapxnet.dataopsdapservice.service.ApiConfigService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApiConfigServiceImpl implements ApiConfigService {

    private final ApiConfigMapper apiConfigMapper;

    public ApiConfigServiceImpl(ApiConfigMapper apiConfigMapper) {
        this.apiConfigMapper = apiConfigMapper;
    }

    // ==================== ApiConfig ====================

    @Override
    public List<ApiConfig> listAllConfigs() {
        return apiConfigMapper.findAllConfigs();
    }

    @Override
    public ApiConfig getConfigById(Long id) {
        ApiConfig config = apiConfigMapper.findConfigById(id);
        if (config == null) {
            throw new IllegalArgumentException("ApiConfig not found: " + id);
        }
        return config;
    }

    @Override
    public ApiConfig createConfig(ApiConfig apiConfig) {
        apiConfig.setUid(UUID.randomUUID().toString());
        if (apiConfig.getStatus() == null) {
            apiConfig.setStatus("draft");
        }
        if (apiConfig.getRateLimit() == null) {
            apiConfig.setRateLimit(100);
        }
        if (apiConfig.getCacheTtl() == null) {
            apiConfig.setCacheTtl(0);
        }
        apiConfigMapper.insertConfig(apiConfig);
        return apiConfig;
    }

    @Override
    public ApiConfig updateConfig(ApiConfig apiConfig) {
        apiConfigMapper.updateConfig(apiConfig);
        return apiConfigMapper.findConfigById(apiConfig.getId());
    }

    @Override
    public void deleteConfig(Long id) {
        apiConfigMapper.deleteConfigById(id);
    }

    @Override
    public ApiConfig publishConfig(Long id) {
        ApiConfig config = getConfigById(id);
        if ("deprecated".equals(config.getStatus())) {
            throw new IllegalArgumentException("Cannot publish a deprecated API config");
        }
        apiConfigMapper.updateConfigStatus(id, "published");
        return apiConfigMapper.findConfigById(id);
    }

    @Override
    public ApiConfig deprecateConfig(Long id) {
        getConfigById(id);
        apiConfigMapper.updateConfigStatus(id, "deprecated");
        return apiConfigMapper.findConfigById(id);
    }

    // ==================== ApiKey ====================

    @Override
    public List<ApiKey> listAllKeys() {
        return apiConfigMapper.findAllKeys();
    }

    @Override
    public ApiKey createKey(ApiKey apiKey) {
        apiKey.setUid(UUID.randomUUID().toString());
        apiKey.setApiKey(UUID.randomUUID().toString().replace("-", ""));
        apiKey.setSecretKey(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        if (apiKey.getStatus() == null) {
            apiKey.setStatus("active");
        }
        apiConfigMapper.insertKey(apiKey);
        return apiKey;
    }

    @Override
    public ApiKey revokeKey(Long id) {
        ApiKey key = apiConfigMapper.findKeyById(id);
        if (key == null) {
            throw new IllegalArgumentException("ApiKey not found: " + id);
        }
        apiConfigMapper.updateKeyStatus(id, "revoked");
        key.setStatus("revoked");
        return key;
    }

    @Override
    public void deleteKey(Long id) {
        apiConfigMapper.deleteKeyById(id);
    }

    // ==================== ApiCallLog ====================

    @Override
    public List<ApiCallLog> listLogs(Long apiConfigId) {
        if (apiConfigId != null) {
            return apiConfigMapper.findLogsByApiConfigId(apiConfigId);
        }
        return apiConfigMapper.findAllLogs();
    }

    @Override
    public Map<String, Object> getCallStats(Long apiConfigId) {
        if (apiConfigId != null) {
            return apiConfigMapper.getCallStatsByApiConfigId(apiConfigId);
        }
        return apiConfigMapper.getCallStats();
    }
}
