package com.synapxnet.dataopstskservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopstskservice.entity.NodeInstance;
import com.synapxnet.dataopstskservice.entity.TaskInstance;
import com.synapxnet.dataopstskservice.entity.Workflow;
import com.synapxnet.dataopstskservice.entity.WorkflowNode;
import com.synapxnet.dataopstskservice.service.WorkflowService;
import com.synapxnet.goai.contract.AgentContractException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 将任务实例和批量节点状态转换为脱敏、稳定排序的执行证据。
 */
@Service
public class WorkflowInstanceEvidenceService {

    private static final int LOG_LIMIT = 1000;
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|api[_-]?key|secret)\\s*[:=]\\s*[^\\s,;]+"
                    + "|(?:jdbc|mysql|postgresql)://[^\\s]+"
                    + "|\\b1[3-9]\\d{9}\\b"
                    + "|[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;

    /**
     * 创建任务实例证据服务。
     *
     * @param workflowService 现有工作流领域服务
     * @param objectMapper 节点配置 JSON 解析器
     */
    public WorkflowInstanceEvidenceService(WorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据稳定 UID 获取任务和节点证据；日志只有显式请求时才从数据库读取。
     *
     * @param instanceUid 任务实例 UID
     * @param includeLogSummary 是否读取并返回脱敏日志摘要
     * @return 任务实例证据
     */
    public WorkflowInstanceEvidence get(String instanceUid, boolean includeLogSummary) {
        if (instanceUid == null || instanceUid.isBlank()) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "instanceUid 不能为空");
        }
        TaskInstance instance;
        try {
            instance = workflowService.getInstanceByUid(instanceUid);
        } catch (IllegalArgumentException exception) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "任务实例不存在");
        }
        Workflow workflow = workflowService.getById(instance.getWorkflowId());
        List<String> warnings = new java.util.ArrayList<>();
        List<NodeEvidence> nodes = workflowService.getNodeInstances(instance.getId(), includeLogSummary).stream()
                .map(node -> toNode(node, includeLogSummary))
                .sorted(Comparator.comparing(NodeEvidence::startedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<OutputAssetReference> outputAssets = outputAssets(
                workflowService.getNodes(workflow.getId()), warnings);
        if (!includeLogSummary) {
            warnings.add("LOG_SUMMARY_NOT_REQUESTED");
        }
        return new WorkflowInstanceEvidence(
                instance.getUid(), workflow.getUid(), workflow.getName(), instance.getStatus(),
                instance.getTriggerType(), toInstant(instance.getStartTime()), toInstant(instance.getEndTime()),
                durationMs(instance.getStartTime(), instance.getEndTime()), nodes, outputAssets,
                List.copyOf(warnings));
    }

    /**
     * 将节点 Entity 转换为脱敏证据。
     *
     * @param node 节点实例
     * @param includeLogSummary 是否包含日志摘要
     * @return 节点证据
     */
    private NodeEvidence toNode(NodeInstance node, boolean includeLogSummary) {
        return new NodeEvidence(
                node.getNodeKey(), node.getStatus(), toInstant(node.getStartTime()), toInstant(node.getEndTime()),
                durationMs(node.getStartTime(), node.getEndTime()),
                includeLogSummary ? sanitize(node.getLogContent()) : null);
    }

    /**
     * 先脱敏再截断节点日志，隐藏凭据、连接串、手机号和邮箱。
     *
     * @param value 原始节点日志
     * @return 脱敏且受长度限制的摘要
     */
    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = SECRET_PATTERN.matcher(value).replaceAll("[REDACTED]");
        return sanitized.length() <= LOG_LIMIT ? sanitized : sanitized.substring(0, LOG_LIMIT);
    }

    /**
     * 从持久化节点配置提取产出资产引用，坏 JSON 只产生 warning。
     *
     * @param workflowNodes 工作流节点定义
     * @param warnings 警告收集器
     * @return 按 assetUid 去重且稳定排序的产出引用
     */
    private List<OutputAssetReference> outputAssets(
            List<WorkflowNode> workflowNodes,
            List<String> warnings) {
        Map<String, OutputAssetReference> values = new LinkedHashMap<>();
        for (WorkflowNode node : workflowNodes) {
            if (node.getConfigJson() == null || node.getConfigJson().isBlank()) {
                continue;
            }
            try {
                JsonNode config = objectMapper.readTree(node.getConfigJson());
                String assetUid = text(config, "outputAssetUid");
                if (assetUid == null && "publish".equalsIgnoreCase(node.getNodeKey())) {
                    assetUid = text(config, "assetRef");
                }
                if (assetUid != null) {
                    values.put(assetUid, new OutputAssetReference(
                            assetUid, text(config, "schemaSnapshotUid")));
                }
            } catch (Exception exception) {
                warnings.add("NODE_CONFIG_INVALID_JSON:" + node.getNodeKey());
            }
        }
        return values.values().stream()
                .sorted(Comparator.comparing(OutputAssetReference::assetUid)).toList();
    }

    /**
     * 读取受限 JSON 文本；非文本、空白或超长内容返回 null。
     *
     * @param node JSON 对象
     * @param field 字段名
     * @return 有界文本或 null
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) {
            return null;
        }
        String text = value.textValue().trim();
        return text.isEmpty() || text.length() > 128 ? null : text;
    }

    /** 计算起止时间间隔；缺失时间时返回 null。 */
    private Long durationMs(LocalDateTime start, LocalDateTime end) {
        return start == null || end == null ? null : Duration.between(start, end).toMillis();
    }

    /** 将数据库 UTC 本地时间转换为 RFC 3339。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /** 表示节点执行证据。 */
    public record NodeEvidence(
            String nodeKey,
            String status,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            String logSummary) {
    }

    /** 表示任务产出资产引用；无可靠记录时返回空列表。 */
    public record OutputAssetReference(String assetUid, String schemaSnapshotUid) {
    }

    /** 表示可跨平台引用的工作流实例证据。 */
    public record WorkflowInstanceEvidence(
            String instanceUid,
            String workflowUid,
            String workflowName,
            String status,
            String triggerType,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            List<NodeEvidence> nodes,
            List<OutputAssetReference> outputAssets,
            List<String> warnings) {
    }
}
