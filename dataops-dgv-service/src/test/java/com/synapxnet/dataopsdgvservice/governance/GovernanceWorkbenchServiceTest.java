/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：验证聚合数据源与授权过滤。Purpose: Test scoped workbench aggregation and source failures.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.governance;

import com.synapxnet.dataopsdgvservice.agent.AgentDgvMapper;
import com.synapxnet.dataopsdgvservice.agent.AgentDgvEvidenceService;
import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.DataOpsResourceScopes;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GovernanceWorkbenchServiceTest {
    private final AgentDgvMapper catalog = mock(AgentDgvMapper.class);
    private final AgentDgvEvidenceService evidence = mock(AgentDgvEvidenceService.class);
    private final GovernanceWorkbenchMapper quality = mock(GovernanceWorkbenchMapper.class);
    private final GovernanceWorkbenchService service = new GovernanceWorkbenchService(catalog, evidence, quality);
    private final DataOpsResourceScopes.Scope scope = new DataOpsResourceScopes.Scope("s", "w", "t", "d", "team", "fixture", List.of("a"), List.of("r"), List.of());

    /** 无授权请求应在任何数据库读取之前拒绝。 Reject an ungranted requested asset before any database access. */
    @Test void rejectsForeignAssetBeforeReading() {
        assertThrows(AgentContractException.class, () -> service.read(scope, "foreign", null, "both", 2));
        verifyNoInteractions(catalog, evidence, quality);
    }

    /** 源连接失败不能显示成空目录成功。 A source outage must remain unavailable rather than an empty success. */
    @Test void distinguishesEmptyAndUnavailable() {
        assertEquals("empty", service.read(scope, null, null, "both", 2).availability());
        when(catalog.findTableByUid("a")).thenThrow(new IllegalStateException("private database address"));
        var result = service.read(scope, null, null, "both", 2);
        assertEquals("unavailable", result.availability());
        assertFalse(result.toString().contains("private database address"));
    }

    /** 错误质量绑定不能泄漏，目录成功不被其他源失败覆盖。 Exclude foreign quality bindings and preserve successful catalog data during other source failures. */
    @Test void rejectsForeignQualityBindingAndKeepsCatalog() {
        MetaTable asset = new MetaTable(); asset.setUid("a"); asset.setSchemaName("analytics"); asset.setTableName("orders");
        when(catalog.findTableByUid("a")).thenReturn(asset);
        when(evidence.getSchemaSnapshot("a", null, null)).thenThrow(new AgentContractException(404, "RESOURCE_NOT_FOUND", "missing"));
        when(quality.quality("r")).thenReturn(new GovernanceWorkbenchMapper.QualityRow("r", "foreign", "rule", "passed", null, 10L, 0L, null));
        var result = service.read(scope, null, null, "both", 2);
        assertEquals(1, result.assets().size()); assertTrue(result.quality().isEmpty());
        assertEquals("fixture", result.executionMode()); assertEquals("1.0.0", result.schemaVersion());
        verify(catalog).findTableByUid("a");
    }

    /** 已授权但不存在的目标不能静默切换到其他资产。 An authorized missing target must not silently select another asset. */
    @Test void rejectsMissingExplicitTarget() {
        var multiple = new DataOpsResourceScopes.Scope("s", "w", "t", "d", "team", "fixture", List.of("a", "b"), List.of(), List.of());
        MetaTable other = new MetaTable(); other.setUid("b"); other.setSchemaName("analytics"); other.setTableName("other");
        when(catalog.findTableByUid("b")).thenReturn(other);
        var exception = assertThrows(AgentContractException.class, () -> service.read(multiple, "a", null, "both", 2));
        assertEquals(404, exception.getHttpStatus());
        verifyNoInteractions(evidence, quality);
    }

    /** 搜索只过滤候选列表，不更改显式目标。 Search filters candidates without changing the explicit evidence target. */
    @Test void retainsExplicitTargetOutsideSearchResults() {
        MetaTable asset = new MetaTable(); asset.setUid("a"); asset.setSchemaName("analytics"); asset.setTableName("orders");
        when(catalog.findTableByUid("a")).thenReturn(asset);
        var result = service.read(scope, "a", "no-matching-name", "both", 2);
        assertTrue(result.assets().isEmpty());
        assertEquals("a", result.selectedAssetUid());
        verify(evidence).getSchemaSnapshot("a", null, null);
    }

    /** 显式目录读取失败保留不可用，不选择替代资产。 Preserve unavailable status when an explicit target cannot be read. */
    @Test void doesNotFallbackAfterCatalogFailure() {
        when(catalog.findTableByUid("a")).thenThrow(new IllegalStateException("outage"));
        var result = service.read(scope, "a", null, "both", 2);
        assertEquals("unavailable", result.availability());
        assertNull(result.selectedAssetUid());
        verifyNoInteractions(evidence, quality);
    }
}
