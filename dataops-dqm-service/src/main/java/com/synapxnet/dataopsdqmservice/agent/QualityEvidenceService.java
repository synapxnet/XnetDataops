package com.synapxnet.dataopsdqmservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdqmservice.entity.QualityAlert;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.entity.QualityRule;
import com.synapxnet.dataopsdqmservice.mapper.QualityRuleMapper;
import com.synapxnet.goai.contract.AgentContractException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 DQM 报告、规则、告警和契约检查组合为稳定质量证据。
 */
@Service
public class QualityEvidenceService {

    private final QualityRuleMapper qualityRuleMapper;
    private final AgentQualityMapper agentQualityMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建质量证据服务。
     *
     * @param qualityRuleMapper 现有质量领域 Mapper
     * @param agentQualityMapper 契约检查 Mapper
     * @param objectMapper Jackson JSON 解析器
     */
    public QualityEvidenceService(
            QualityRuleMapper qualityRuleMapper,
            AgentQualityMapper agentQualityMapper,
            ObjectMapper objectMapper) {
        this.qualityRuleMapper = qualityRuleMapper;
        this.agentQualityMapper = agentQualityMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据报告 UID 获取真实质量报告证据，坏 JSON 仅降级明细并保留核心字段。
     *
     * @param reportUid 质量报告 UID
     * @return 质量报告证据
     */
    public QualityReportEvidence getReport(String reportUid) {
        if (reportUid == null || reportUid.isBlank()) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "reportUid 不能为空");
        }
        QualityReport report = qualityRuleMapper.findReportByUid(reportUid);
        if (report == null) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "质量报告不存在");
        }
        QualityRule rule = qualityRuleMapper.findRuleById(report.getRuleId());
        ContractCheck contractCheck = agentQualityMapper.findContractCheck(reportUid);
        List<String> warnings = new ArrayList<>();
        ParsedDetail detail = parseDetail(report.getDetailJson(), warnings);
        List<AlertReference> alerts = qualityRuleMapper.findAlertsByReportId(report.getId()).stream()
                .map(this::toAlertReference).toList();
        String assetUid = contractCheck == null ? detail.assetUid() : contractCheck.getAssetUid();
        String snapshotUid = contractCheck == null
                ? detail.schemaSnapshotUid() : contractCheck.getSchemaSnapshotUid();
        return new QualityReportEvidence(
                report.getUid(), rule == null ? null : toRuleSummary(rule), assetUid,
                toInstant(report.getCheckTime()), report.getTotalRows(), report.getFailedRows(),
                normalizePassRate(report.getPassRate()), report.getStatus(), detail.rules(), snapshotUid,
                contractCheck == null ? null : toContractEvidence(contractCheck), alerts, List.copyOf(warnings));
    }

    /**
     * 使用 Jackson 解析报告明细，禁止手工截取 JSON 字符串。
     *
     * @param detailJson 报告明细 JSON
     * @param warnings 警告收集器
     * @return 解析后的明确 DTO
     */
    private ParsedDetail parseDetail(String detailJson, List<String> warnings) {
        if (detailJson == null || detailJson.isBlank()) {
            warnings.add("REPORT_DETAIL_EMPTY");
            return new ParsedDetail(null, null, List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(detailJson);
            List<RuleResult> results = new ArrayList<>();
            JsonNode rules = root.path("rules");
            if (rules.isArray()) {
                for (JsonNode rule : rules) {
                    results.add(new RuleResult(
                            text(rule, "ruleUid"), text(rule, "name"), text(rule, "status"),
                            rule.path("failedRows").asLong(0L), text(rule, "message")));
                }
            }
            return new ParsedDetail(
                    text(root, "assetUid"), text(root, "schemaSnapshotUid"), List.copyOf(results));
        } catch (Exception exception) {
            warnings.add("REPORT_DETAIL_INVALID_JSON");
            return new ParsedDetail(null, null, List.of());
        }
    }

    /**
     * 安全读取 JSON 文本字段，缺失时返回 null。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return 文本值或 null
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 将历史百分数或 0 到 1 比例统一为 0 到 1。
     *
     * @param value 数据库存储通过率
     * @return 0 到 1 的比例
     */
    private BigDecimal normalizePassRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = value.compareTo(BigDecimal.ONE) > 0
                ? value.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP) : value;
        return normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    /** 将规则 Entity 转换为证据摘要。 */
    private RuleSummary toRuleSummary(QualityRule rule) {
        return new RuleSummary(
                rule.getUid(), rule.getName(), rule.getRuleType(), rule.getSeverity(),
                rule.getTableName(), rule.getColumnName());
    }

    /** 将质量告警 Entity 转换为受控引用。 */
    private AlertReference toAlertReference(QualityAlert alert) {
        return new AlertReference(
                alert.getUid(), alert.getAlertLevel(), alert.getStatus(), toInstant(alert.getTriggeredAt()));
    }

    /** 将契约检查记录转换为稳定证据。 */
    private ContractCheckEvidence toContractEvidence(ContractCheck value) {
        return new ContractCheckEvidence(
                value.getUid(), value.getContractRef(), value.getExpectedFieldCount(),
                value.getActualFieldCount(), value.getStatus(), toInstant(value.getCheckedAt()),
                value.getIncidentId(), value.getTraceId());
    }

    /** 将数据库 UTC 本地时间转换为 RFC 3339。 */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /** 表示质量规则摘要。 */
    public record RuleSummary(
            String ruleUid,
            String name,
            String type,
            String severity,
            String tableName,
            String columnName) {
    }

    /** 表示单条规则执行结果。 */
    public record RuleResult(String ruleUid, String name, String status, long failedRows, String message) {
    }

    /** 表示 DataOps 已知模型契约引用的检查事实。 */
    public record ContractCheckEvidence(
            String checkUid,
            String contractRef,
            int expectedFieldCount,
            int actualFieldCount,
            String status,
            Instant checkedAt,
            String incidentId,
            String traceId) {
    }

    /** 表示关联质量告警的受控引用。 */
    public record AlertReference(String alertUid, String level, String status, Instant triggeredAt) {
    }

    /** 表示可跨平台引用的质量报告证据。 */
    public record QualityReportEvidence(
            String reportUid,
            RuleSummary rule,
            String assetUid,
            Instant checkedAt,
            long totalRows,
            long failedRows,
            BigDecimal passRate,
            String qualityStatus,
            List<RuleResult> ruleResults,
            String schemaSnapshotUid,
            ContractCheckEvidence contractCheck,
            List<AlertReference> alerts,
            List<String> warnings) {
    }

    /** 表示从 detailJson 安全解析的内部结构。 */
    private record ParsedDetail(String assetUid, String schemaSnapshotUid, List<RuleResult> rules) {
    }
}
