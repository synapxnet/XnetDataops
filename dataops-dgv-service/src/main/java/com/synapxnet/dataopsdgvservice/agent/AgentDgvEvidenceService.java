package com.synapxnet.dataopsdgvservice.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdgvservice.entity.DataLineage;
import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import com.synapxnet.dataopsdgvservice.mapper.MetaTableMapper;
import com.synapxnet.goai.contract.AgentContractException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 从 DGV 持久化事实构造 Schema 和有界血缘证据。
 */
@Service
public class AgentDgvEvidenceService {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_NODES = 100;
    private static final Duration MAX_QUERY_DURATION = Duration.ofSeconds(2);
    private final AgentDgvMapper agentMapper;
    private final MetaTableMapper metaTableMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建 DGV 证据服务。
     *
     * @param agentMapper Agent 专用直接查询
     * @param metaTableMapper 现有元数据与血缘 Mapper
     * @param objectMapper Jackson 结构化 JSON 解析器
     */
    public AgentDgvEvidenceService(
            AgentDgvMapper agentMapper,
            MetaTableMapper metaTableMapper,
            ObjectMapper objectMapper) {
        this.agentMapper = agentMapper;
        this.metaTableMapper = metaTableMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 按版本、时间点或最新规则读取 Schema 快照。
     *
     * @param assetUid 资产 UID
     * @param schemaVersion 可选 Schema 版本
     * @param observedAt 可选 UTC 时间点
     * @return 不含样本值的 Schema 证据
     */
    public SchemaSnapshotEvidence getSchemaSnapshot(
            String assetUid,
            String schemaVersion,
            Instant observedAt) {
        if (assetUid == null || assetUid.isBlank()) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "assetUid 不能为空");
        }
        if (schemaVersion != null && !schemaVersion.isBlank() && observedAt != null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "schemaVersion 和 observedAt 不能同时提供");
        }
        SchemaSnapshot snapshot;
        if (schemaVersion != null && !schemaVersion.isBlank()) {
            snapshot = agentMapper.findSnapshotByVersion(assetUid, schemaVersion);
        } else if (observedAt != null) {
            snapshot = agentMapper.findSnapshotAt(assetUid, LocalDateTime.ofInstant(observedAt, ZoneOffset.UTC));
        } else {
            snapshot = agentMapper.findLatestSnapshot(assetUid);
        }
        if (snapshot == null) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "Schema 快照不存在");
        }
        List<SchemaField> fields = parseFields(snapshot.getSchemaJson());
        if (fields.size() != snapshot.getFieldCount()) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "Schema 快照字段数与持久化计数不一致");
        }
        return new SchemaSnapshotEvidence(
                snapshot.getUid(), snapshot.getAssetUid(), snapshot.getAssetName(), snapshot.getSchemaVersion(),
                snapshot.getFieldCount(), snapshot.getSchemaHash(), fields, snapshot.getSourceType(),
                toInstant(snapshot.getCapturedAt()));
    }

    /**
     * 使用有界 BFS 查询上下游血缘，检测环并保证稳定排序。
     *
     * @param assetUid 根资产 UID
     * @param direction UPSTREAM、DOWNSTREAM 或 BOTH
     * @param requestedDepth 请求深度，最大 5
     * @return 有界血缘证据
     */
    public LineageEvidence getLineage(String assetUid, Direction direction, Integer requestedDepth) {
        if (assetUid == null || assetUid.isBlank() || direction == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "assetUid 和 direction 不能为空");
        }
        int depth = requestedDepth == null ? 2 : requestedDepth;
        if (depth < 1 || depth > MAX_DEPTH) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "depth 必须在 1 到 5 之间");
        }
        MetaTable root = agentMapper.findTableByUid(assetUid);
        if (root == null) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "血缘根资产不存在");
        }
        return traverse(root, direction, depth);
    }

    /**
     * 执行有限节点、深度和时间的 BFS，禁止无限递归。
     *
     * @param root 根资产
     * @param direction 查询方向
     * @param maxDepth 最大深度
     * @return 血缘证据
     */
    private LineageEvidence traverse(MetaTable root, Direction direction, int maxDepth) {
        Instant deadline = Instant.now().plus(MAX_QUERY_DURATION);
        Queue<NodeDepth> queue = new ArrayDeque<>();
        queue.add(new NodeDepth(root, 0));
        Set<Long> visited = new HashSet<>();
        Map<Long, LineageNode> nodes = new HashMap<>();
        Map<String, LineageEdge> edges = new HashMap<>();
        boolean cycleDetected = false;
        boolean truncated = false;
        while (!queue.isEmpty()) {
            if (Instant.now().isAfter(deadline) || nodes.size() >= MAX_NODES) {
                truncated = true;
                break;
            }
            NodeDepth current = queue.remove();
            if (!visited.add(current.table().getId())) {
                cycleDetected = true;
                continue;
            }
            nodes.put(current.table().getId(), toNode(current.table()));
            if (current.depth() >= maxDepth) {
                if (!metaTableMapper.findLineageByTableId(current.table().getId()).isEmpty()) {
                    truncated = true;
                }
                continue;
            }
            for (DataLineage relation : metaTableMapper.findLineageByTableId(current.table().getId())) {
                Long neighborId = neighborId(current.table().getId(), relation, direction);
                if (neighborId == null) {
                    continue;
                }
                MetaTable source = metaTableMapper.findTableById(relation.getSourceTableId());
                MetaTable target = metaTableMapper.findTableById(relation.getTargetTableId());
                if (source == null || target == null) {
                    continue;
                }
                edges.put(relation.getUid(), toEdge(relation, source, target));
                MetaTable neighbor = neighborId.equals(source.getId()) ? source : target;
                if (visited.contains(neighborId)) {
                    cycleDetected = true;
                } else {
                    queue.add(new NodeDepth(neighbor, current.depth() + 1));
                }
            }
        }
        List<LineageNode> sortedNodes = nodes.values().stream()
                .sorted(Comparator.comparing(LineageNode::assetUid)).toList();
        List<LineageEdge> sortedEdges = edges.values().stream()
                .sorted(Comparator.comparing(LineageEdge::edgeUid)).toList();
        return new LineageEvidence(root.getUid(), sortedNodes, sortedEdges, truncated, cycleDetected);
    }

    /**
     * 根据查询方向确定当前关系的下一资产。
     *
     * @param currentId 当前资产数字 ID
     * @param relation 血缘关系
     * @param direction 查询方向
     * @return 下一资产 ID，不符合方向时返回 null
     */
    private Long neighborId(Long currentId, DataLineage relation, Direction direction) {
        if (direction != Direction.DOWNSTREAM && currentId.equals(relation.getTargetTableId())) {
            return relation.getSourceTableId();
        }
        if (direction != Direction.UPSTREAM && currentId.equals(relation.getSourceTableId())) {
            return relation.getTargetTableId();
        }
        return null;
    }

    /**
     * 将元数据表转换为稳定血缘节点。
     *
     * @param table 元数据表
     * @return 血缘节点
     */
    private LineageNode toNode(MetaTable table) {
        SchemaSnapshot snapshot = agentMapper.findLatestSnapshot(table.getUid());
        return new LineageNode(
                table.getUid(), table.getSchemaName() + "." + table.getTableName(), table.getTableType(),
                snapshot == null ? null : snapshot.getUid());
    }

    /**
     * 将血缘 Entity 转换为稳定边 DTO。
     *
     * @param relation 血缘关系
     * @param source 源资产
     * @param target 目标资产
     * @return 血缘边
     */
    private LineageEdge toEdge(DataLineage relation, MetaTable source, MetaTable target) {
        return new LineageEdge(
                relation.getUid(), source.getUid(), target.getUid(), relation.getTransformType(),
                relation.getWorkflowId() == null ? null : agentMapper.findWorkflowUid(relation.getWorkflowId()));
    }

    /**
     * 使用 Jackson 将字段 JSON 映射为明确 DTO。
     *
     * @param schemaJson 持久化字段 JSON
     * @return 按 ordinal 排序的字段列表
     */
    private List<SchemaField> parseFields(String schemaJson) {
        try {
            List<SchemaField> fields = objectMapper.readValue(schemaJson, new TypeReference<>() { });
            return fields.stream().sorted(Comparator.comparingInt(SchemaField::ordinal)).toList();
        } catch (JsonProcessingException exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "Schema 快照内容无法解析");
        }
    }

    /** 将数据库 UTC 本地时间转换为 RFC 3339。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /** 表示血缘查询方向。 */
    public enum Direction {
        UPSTREAM,
        DOWNSTREAM,
        BOTH
    }

    /** 表示不含样本值的 Schema 字段。 */
    public record SchemaField(String name, String type, boolean nullable, int ordinal, String description) {
    }

    /** 表示持久化 Schema 快照证据。 */
    public record SchemaSnapshotEvidence(
            String snapshotUid,
            String assetUid,
            String assetName,
            String schemaVersion,
            int fieldCount,
            String schemaHash,
            List<SchemaField> fields,
            String source,
            Instant capturedAt) {
    }

    /** 表示稳定资产血缘节点。 */
    public record LineageNode(String assetUid, String name, String type, String schemaSnapshotUid) {
    }

    /** 表示稳定资产血缘边。 */
    public record LineageEdge(
            String edgeUid,
            String sourceAssetUid,
            String targetAssetUid,
            String transformType,
            String workflowUid) {
    }

    /** 表示有界血缘查询结果。 */
    public record LineageEvidence(
            String rootAssetUid,
            List<LineageNode> nodes,
            List<LineageEdge> edges,
            boolean truncated,
            boolean cycleDetected) {
    }

    /** 表示 BFS 队列中的资产和深度。 */
    private record NodeDepth(MetaTable table, int depth) {
    }
}
