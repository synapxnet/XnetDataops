package com.synapxnet.dataopsdasservice.service.impl;

import com.synapxnet.dataopsdasservice.entity.AssetAccessRecord;
import com.synapxnet.dataopsdasservice.entity.AssetClassification;
import com.synapxnet.dataopsdasservice.entity.DataAsset;
import com.synapxnet.dataopsdasservice.mapper.DataAssetMapper;
import com.synapxnet.dataopsdasservice.service.DataAssetService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataAssetServiceImpl implements DataAssetService {

    private final DataAssetMapper dataAssetMapper;

    public DataAssetServiceImpl(DataAssetMapper dataAssetMapper) {
        this.dataAssetMapper = dataAssetMapper;
    }

    // ==================== DataAsset ====================

    @Override
    public List<DataAsset> listAssets(String domain, String category) {
        if (domain != null && !domain.isEmpty() && category != null && !category.isEmpty()) {
            return dataAssetMapper.findAssetsByDomainAndCategory(domain, category);
        } else if (domain != null && !domain.isEmpty()) {
            return dataAssetMapper.findAssetsByDomain(domain);
        } else if (category != null && !category.isEmpty()) {
            return dataAssetMapper.findAssetsByCategory(category);
        }
        return dataAssetMapper.findAllAssets();
    }

    @Override
    public DataAsset getAssetById(Long id) {
        DataAsset asset = dataAssetMapper.findAssetById(id);
        if (asset == null) {
            throw new IllegalArgumentException("DataAsset not found: " + id);
        }
        return asset;
    }

    @Override
    public DataAsset createAsset(DataAsset asset) {
        asset.setUid(UUID.randomUUID().toString());
        if (asset.getStatus() == null) {
            asset.setStatus("active");
        }
        if (asset.getAccessLevel() == null) {
            asset.setAccessLevel("internal");
        }
        dataAssetMapper.insertAsset(asset);
        return asset;
    }

    @Override
    public DataAsset updateAsset(DataAsset asset) {
        dataAssetMapper.updateAsset(asset);
        return dataAssetMapper.findAssetById(asset.getId());
    }

    @Override
    public void deleteAsset(Long id) {
        dataAssetMapper.deleteAssetById(id);
    }

    @Override
    public Map<String, Object> getAssetStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("byDomain", toCountMap(dataAssetMapper.countByDomain()));
        stats.put("byCategory", toCountMap(dataAssetMapper.countByCategory()));
        stats.put("byAccessLevel", toCountMap(dataAssetMapper.countByAccessLevel()));
        return stats;
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            // The first non-cnt column is the key
            String key = null;
            Long cnt = null;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if ("cnt".equals(entry.getKey())) {
                    cnt = ((Number) entry.getValue()).longValue();
                } else {
                    key = entry.getValue() != null ? entry.getValue().toString() : "null";
                }
            }
            if (key != null && cnt != null) {
                map.put(key, cnt);
            }
        }
        return map;
    }

    // ==================== AssetClassification ====================

    @Override
    public List<AssetClassification> listClassifications() {
        return dataAssetMapper.findAllClassifications();
    }

    @Override
    public AssetClassification createClassification(AssetClassification classification) {
        dataAssetMapper.insertClassification(classification);
        return classification;
    }

    @Override
    public AssetClassification updateClassification(AssetClassification classification) {
        dataAssetMapper.updateClassification(classification);
        return dataAssetMapper.findClassificationById(classification.getId());
    }

    @Override
    public void deleteClassification(Long id) {
        dataAssetMapper.deleteClassificationById(id);
    }

    // ==================== AssetAccessRecord ====================

    @Override
    public List<AssetAccessRecord> listAccessRecords(Long assetId) {
        return dataAssetMapper.findAccessRecordsByAssetId(assetId);
    }

    @Override
    public AssetAccessRecord createAccessRecord(Long assetId, AssetAccessRecord record) {
        record.setUid(UUID.randomUUID().toString());
        record.setAssetId(assetId);
        if (record.getAccessTime() == null) {
            record.setAccessTime(LocalDateTime.now());
        }
        dataAssetMapper.insertAccessRecord(record);
        return record;
    }
}
