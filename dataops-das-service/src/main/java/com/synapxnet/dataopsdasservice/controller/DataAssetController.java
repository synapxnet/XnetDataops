package com.synapxnet.dataopsdasservice.controller;

import com.synapxnet.dataopsdasservice.common.Result;
import com.synapxnet.dataopsdasservice.entity.AssetAccessRecord;
import com.synapxnet.dataopsdasservice.entity.AssetClassification;
import com.synapxnet.dataopsdasservice.entity.DataAsset;
import com.synapxnet.dataopsdasservice.service.DataAssetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/das")
public class DataAssetController {

    private final DataAssetService dataAssetService;

    public DataAssetController(DataAssetService dataAssetService) {
        this.dataAssetService = dataAssetService;
    }

    // ==================== DataAsset ====================

    @GetMapping("/assets")
    public Result<List<DataAsset>> listAssets(@RequestParam(required = false) String domain,
                                              @RequestParam(required = false) String category) {
        return Result.success(dataAssetService.listAssets(domain, category));
    }

    @GetMapping("/assets/{id}")
    public Result<DataAsset> getAsset(@PathVariable("id") Long id) {
        return Result.success(dataAssetService.getAssetById(id));
    }

    @PostMapping("/assets")
    public Result<DataAsset> createAsset(@RequestBody DataAsset asset) {
        return Result.success(dataAssetService.createAsset(asset));
    }

    @PutMapping("/assets/{id}")
    public Result<DataAsset> updateAsset(@PathVariable("id") Long id, @RequestBody DataAsset asset) {
        asset.setId(id);
        return Result.success(dataAssetService.updateAsset(asset));
    }

    @DeleteMapping("/assets/{id}")
    public Result<Void> deleteAsset(@PathVariable("id") Long id) {
        dataAssetService.deleteAsset(id);
        return Result.success();
    }

    @GetMapping("/assets/{id}/access-records")
    public Result<List<AssetAccessRecord>> listAccessRecords(@PathVariable("id") Long id) {
        return Result.success(dataAssetService.listAccessRecords(id));
    }

    @PostMapping("/assets/{id}/access-records")
    public Result<AssetAccessRecord> createAccessRecord(@PathVariable("id") Long id,
                                                        @RequestBody AssetAccessRecord record) {
        return Result.success(dataAssetService.createAccessRecord(id, record));
    }

    @GetMapping("/assets/stats")
    public Result<Map<String, Object>> getAssetStats() {
        return Result.success(dataAssetService.getAssetStats());
    }

    // ==================== AssetClassification ====================

    @GetMapping("/classifications")
    public Result<List<AssetClassification>> listClassifications() {
        return Result.success(dataAssetService.listClassifications());
    }

    @PostMapping("/classifications")
    public Result<AssetClassification> createClassification(@RequestBody AssetClassification classification) {
        return Result.success(dataAssetService.createClassification(classification));
    }

    @PutMapping("/classifications/{id}")
    public Result<AssetClassification> updateClassification(@PathVariable("id") Long id,
                                                            @RequestBody AssetClassification classification) {
        classification.setId(id);
        return Result.success(dataAssetService.updateClassification(classification));
    }

    @DeleteMapping("/classifications/{id}")
    public Result<Void> deleteClassification(@PathVariable("id") Long id) {
        dataAssetService.deleteClassification(id);
        return Result.success();
    }
}
