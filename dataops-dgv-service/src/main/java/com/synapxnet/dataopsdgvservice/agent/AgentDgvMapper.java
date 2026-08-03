package com.synapxnet.dataopsdgvservice.agent;

import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 为 Agent Schema 与血缘证据提供直接、参数化的领域查询。
 */
@Mapper
public interface AgentDgvMapper {

    /**
     * 查询资产最新 Schema 快照。
     *
     * @param assetUid 资产 UID
     * @return 最新快照，不存在时返回 null
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
     */
    @Select("SELECT * FROM xnet_dataops_dgv_meta_table WHERE uid = #{assetUid} LIMIT 1")
    MetaTable findTableByUid(@Param("assetUid") String assetUid);

    /**
     * 查询工作流稳定 UID，跨模块只读取引用字段。
     *
     * @param workflowId 工作流数字 ID
     * @return 工作流 UID，不存在时返回 null
     */
    @Select("SELECT uid FROM xnet_dataops_tsk_workflow WHERE id = #{workflowId}")
    String findWorkflowUid(@Param("workflowId") Long workflowId);
}
