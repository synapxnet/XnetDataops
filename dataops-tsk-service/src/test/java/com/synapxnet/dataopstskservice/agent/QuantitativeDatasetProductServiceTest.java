/*
 * Copyright (C) 2026 Synapxnet. All rights reserved.
 * This file is Synapxnet Proprietary and Confidential. It is strictly
 * forbidden to copy, distribute, or use without explicit authorization.
 * 治理执行与只读取证 / Governed execution and read-only evidence.
 * Author: maoyo | Department: 研发部 | Date: 2026-09-17
 * Version: 1.3.0 | Security Level: INTERNAL
 * __version__: 1.3.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
 * __maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopstskservice.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证量化数据产品路由不会误接管其他业务数据集。 */
class QuantitativeDatasetProductServiceTest {

    /** 确认服务只识别固定 A 股研究数据产品版本。
     * English: Supports Only Published Ashare Product.
     */
    @Test
    void supportsOnlyPublishedAshareProduct() {
        QuantitativeDatasetProductService service = new QuantitativeDatasetProductService(
                "jdbc:postgresql://127.0.0.1:1/test", "test", "test");
        assertTrue(service.supports("a-share-factor-demo-v1"));
        assertFalse(service.supports("recommendation-dcn-v1"));
        assertFalse(service.supports("risk-feature-backfill-v2"));
    }
}
