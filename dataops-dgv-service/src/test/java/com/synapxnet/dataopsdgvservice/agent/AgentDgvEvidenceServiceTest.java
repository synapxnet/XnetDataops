/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：恢复并约束原生只读取证契约。Purpose: Restore bounded native evidence contracts.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
Historical SynapXnet source restored selectively; existing repository license retained.
 */
package com.synapxnet.dataopsdgvservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdgvservice.entity.DataLineage;
import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import com.synapxnet.dataopsdgvservice.mapper.MetaTableMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 Schema 结构化解析与血缘 BFS 的环、深度和稳定排序。
 * English: Read and project persisted lineage with explicit direction, bounded traversal and stable asset identities.
 */
class AgentDgvEvidenceServiceTest {

    private final AgentDgvMapper agentMapper = mock(AgentDgvMapper.class);
    private final MetaTableMapper metaTableMapper = mock(MetaTableMapper.class);
    private final AgentDgvEvidenceService service = new AgentDgvEvidenceService(
            agentMapper, metaTableMapper, new ObjectMapper());

    /** Schema 字段必须按 ordinal 排序且不包含样本值。
 * English: Read or verify typed schema evidence from persisted snapshots while excluding sample values.
 */
    @Test
    void returnsSortedSchemaFields() {
        SchemaSnapshot snapshot = new SchemaSnapshot();
        snapshot.setUid("schema_risk_features_120");
        snapshot.setAssetUid("asset_risk_features_prod");
        snapshot.setAssetName("prod.risk_features");
        snapshot.setSchemaVersion("120");
        snapshot.setFieldCount(2);
        snapshot.setSchemaHash("db316857073f557d89c4b3fb03e082c53a33e2d50f616d27b86ef4bac6b5dbd7");
        snapshot.setSchemaJson("""
                [{"name":"event_time","type":"TIMESTAMP","nullable":false,"ordinal":2,"description":"事件时间"},
                 {"name":"customer_id","type":"STRING","nullable":false,"ordinal":1,"description":"业务主键"}]
                """);
        snapshot.setSourceType("PIPELINE");
        snapshot.setCapturedAt(LocalDateTime.of(2026, 8, 2, 10, 0));
        when(agentMapper.findLatestSnapshot("asset_risk_features_prod")).thenReturn(snapshot);

        AgentDgvEvidenceService.SchemaSnapshotEvidence result = service.getSchemaSnapshot(
                "asset_risk_features_prod", null, null);

        assertEquals(2, result.fieldCount());
        assertEquals("customer_id", result.fields().get(0).name());
        assertEquals("event_time", result.fields().get(1).name());
    }

    /** 有环血缘必须终止、标记 cycle 并按 UID 排序。
 * English: Read and project persisted lineage with explicit direction, bounded traversal and stable asset identities.
 */
    @Test
    void boundsCyclesAndSortsLineage() {
        MetaTable a = table(1L, "asset_a");
        MetaTable b = table(2L, "asset_b");
        MetaTable c = table(3L, "asset_c");
        DataLineage ab = edge("edge_ab", 1L, 2L);
        DataLineage bc = edge("edge_bc", 2L, 3L);
        DataLineage ca = edge("edge_ca", 3L, 1L);
        when(agentMapper.findTableByUid("asset_b")).thenReturn(b);
        when(metaTableMapper.findTableById(1L)).thenReturn(a);
        when(metaTableMapper.findTableById(2L)).thenReturn(b);
        when(metaTableMapper.findTableById(3L)).thenReturn(c);
        when(agentMapper.findBoundedLineage(1L)).thenReturn(List.of(ab, ca));
        when(agentMapper.findBoundedLineage(2L)).thenReturn(List.of(ab, bc));
        when(agentMapper.findBoundedLineage(3L)).thenReturn(List.of(bc, ca));

        AgentDgvEvidenceService.LineageEvidence result = service.getLineage(
                "asset_b", AgentDgvEvidenceService.Direction.BOTH, 5, uid -> true);

        assertTrue(result.cycleDetected());
        assertEquals(List.of("asset_a", "asset_b", "asset_c"),
                result.nodes().stream().map(AgentDgvEvidenceService.LineageNode::assetUid).toList());
        assertEquals(List.of("edge_ab", "edge_bc", "edge_ca"),
                result.edges().stream().map(AgentDgvEvidenceService.LineageEdge::edgeUid).toList());
    }

    /** 单边双向浏览不能误报环，无授权邻居不可出现在节点或边中。 Bidirectional browsing must not invent cycles or expose ungranted neighbors. */
    @Test void excludesForeignNodesAndDoesNotInventCycles() {
        MetaTable a = table(1L, "asset_a"); MetaTable b = table(2L, "asset_b");
        DataLineage ab = edge("edge_ab", 1L, 2L);
        when(agentMapper.findTableByUid("asset_a")).thenReturn(a);
        when(metaTableMapper.findTableById(1L)).thenReturn(a);
        when(metaTableMapper.findTableById(2L)).thenReturn(b);
        when(agentMapper.findBoundedLineage(1L)).thenReturn(List.of(ab));
        when(agentMapper.findBoundedLineage(2L)).thenReturn(List.of(ab));
        var visible = service.getLineage("asset_a", AgentDgvEvidenceService.Direction.BOTH, 3, uid -> true);
        assertFalse(visible.cycleDetected());
        assertEquals(2, visible.nodes().size());
        var restricted = service.getLineage("asset_a", AgentDgvEvidenceService.Direction.BOTH, 3, "asset_a"::equals);
        assertEquals(1, restricted.nodes().size());
        assertTrue(restricted.edges().isEmpty());
    }

    /** 创建最小元数据资产。
 * English: Build a minimal metadata table with the supplied numeric ID and stable UID.
 */
    private MetaTable table(Long id, String uid) {
        MetaTable table = new MetaTable();
        table.setId(id);
        table.setUid(uid);
        table.setSchemaName("prod");
        table.setTableName(uid);
        table.setTableType("table");
        return table;
    }

    /** 创建最小血缘边。
 * English: Build a lineage edge with the supplied UID and numeric endpoint IDs.
 */
    private DataLineage edge(String uid, Long source, Long target) {
        DataLineage edge = new DataLineage();
        edge.setUid(uid);
        edge.setSourceTableId(source);
        edge.setTargetTableId(target);
        edge.setTransformType("etl");
        return edge;
    }
}
