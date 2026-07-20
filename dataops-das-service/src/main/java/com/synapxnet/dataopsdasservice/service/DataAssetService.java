package com.synapxnet.dataopsdasservice.service;

import com.synapxnet.dataopsdasservice.entity.AssetAccessRecord;
import com.synapxnet.dataopsdasservice.entity.AssetClassification;
import com.synapxnet.dataopsdasservice.entity.DataAsset;

import java.util.List;
import java.util.Map;

public interface DataAssetService {

    // DataAsset
    List<DataAsset> listAssets(String domain, String category);
    DataAsset getAssetById(Long id);
    DataAsset createAsset(DataAsset asset);
    DataAsset updateAsset(DataAsset asset);
    void deleteAsset(Long id);
    Map<String, Object> getAssetStats();

    // AssetClassification
    List<AssetClassification> listClassifications();
    AssetClassification createClassification(AssetClassification classification);
    AssetClassification updateClassification(AssetClassification classification);
    void deleteClassification(Long id);

    // AssetAccessRecord
    List<AssetAccessRecord> listAccessRecords(Long assetId);
    AssetAccessRecord createAccessRecord(Long assetId, AssetAccessRecord record);
}
