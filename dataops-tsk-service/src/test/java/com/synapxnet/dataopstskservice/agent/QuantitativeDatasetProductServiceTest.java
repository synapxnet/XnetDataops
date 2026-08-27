package com.synapxnet.dataopstskservice.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证量化数据产品路由不会误接管其他业务数据集。 */
class QuantitativeDatasetProductServiceTest {

    /** 确认服务只识别固定 A 股研究数据产品版本。 */
    @Test
    void supportsOnlyPublishedAshareProduct() {
        QuantitativeDatasetProductService service = new QuantitativeDatasetProductService(
                "jdbc:postgresql://127.0.0.1:1/test", "test", "test");
        assertTrue(service.supports("a-share-factor-demo-v1"));
        assertFalse(service.supports("recommendation-dcn-v1"));
        assertFalse(service.supports("risk-feature-backfill-v2"));
    }
}
