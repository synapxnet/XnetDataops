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

import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import com.synapxnet.dataopsdgvservice.entity.DataLineage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 为 Agent Schema 与血缘证据提供直接、参数化的领域查询。
 * English: Read and project persisted lineage with explicit direction, bounded traversal and stable asset identities.
 */
@Mapper
public interface AgentDgvMapper {
    /** 有界读取相邻关系。 Read at most 201 adjacent relations using a bound parameter. */
    @Select("SELECT * FROM xnet_dataops_dgv_data_lineage WHERE source_table_id=#{id} OR target_table_id=#{id} ORDER BY id LIMIT 201")
    List<DataLineage> findBoundedLineage(@Param("id") Long id);

    /**
     * 查询资产最新 Schema 快照。
     *
     * @param assetUid 资产 UID
     * @return 最新快照，不存在时返回 null
 * English: Query the most recently captured schema snapshot for this asset UID.
 */
    @Select("SELECT * FROM xnet_dataops_dgv_schema_snapshot WHERE asset_uid = #{assetUid} "
            + "ORDER BY captured_at DESC, id DESC LIMIT 1")
    SchemaSnapshot findLatestSnapshot(@Param("assetUid") String assetUid);

    /**
     * 查询资产指定版本的 Schema 快照。
     *
     * @param assetUid 资产 UID
     * @param schemaVersion Schema 版本
     * @return 指定快照，不存在时返回 null
 * English: Query the asset snapshot for the specified schema version.
 */
    @Select("SELECT * FROM xnet_dataops_dgv_schema_snapshot "
            + "WHERE asset_uid = #{assetUid} AND schema_version = #{schemaVersion} LIMIT 1")
    SchemaSnapshot findSnapshotByVersion(
            @Param("assetUid") String assetUid,
            @Param("schemaVersion") String schemaVersion);

    /**
     * 查询指定 UTC 时间点之前最近的 Schema 快照。
     *
     * @param assetUid 资产 UID
     * @param observedAt UTC 时间点
     * @return 时间点快照，不存在时返回 null
 * English: Query the latest asset snapshot captured at or before the UTC cutoff.
 */
    @Select("SELECT * FROM xnet_dataops_dgv_schema_snapshot "
            + "WHERE asset_uid = #{assetUid} AND captured_at <= #{observedAt} "
            + "ORDER BY captured_at DESC, id DESC LIMIT 1")
    SchemaSnapshot findSnapshotAt(
            @Param("assetUid") String assetUid,
            @Param("observedAt") LocalDateTime observedAt);

    /**
     * 根据稳定资产 UID 查询元数据表。
     *
     * @param assetUid 资产 UID
     * @return 元数据表，不存在时返回 null
 * English: Query at most one metadata table by its stable asset UID.
 */
    @Select("SELECT * FROM xnet_dataops_dgv_meta_table WHERE uid = #{assetUid} LIMIT 1")
    MetaTable findTableByUid(@Param("assetUid") String assetUid);

    /**
     * 查询工作流稳定 UID，跨模块只读取引用字段。
     *
     * @param workflowId 工作流数字 ID
     * @return 工作流 UID，不存在时返回 null
 * English: Read only the stable workflow UID needed as a cross-module reference.
 */
    @Select("SELECT uid FROM xnet_dataops_tsk_workflow WHERE id = #{workflowId}")
    String findWorkflowUid(@Param("workflowId") Long workflowId);
}
