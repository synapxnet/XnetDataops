package com.synapxnet.dataopsdqmservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 DataOps 质量报告只读证据工具。
 */
@RestController
public class AgentQualityToolController {

    private static final String TOOL_NAME = "dataops.quality.report.get";
    private final QualityEvidenceService evidenceService;

    /**
     * 创建质量报告工具 Controller。
     *
     * @param evidenceService 质量证据领域服务
     */
    public AgentQualityToolController(QualityEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    /**
     * 获取真实数据库中的质量报告、Schema 关联和契约检查事实。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 质量报告证据
     */
    @PostMapping("/api/agent/v1/tools/dataops.quality.report.get:invoke")
    public AgentContract.ToolResponse<QualityEvidenceService.QualityReportEvidence> invoke(
            @RequestBody AgentContract.ToolRequest<QualityReportArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, TOOL_NAME, body);
        if (body.arguments() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        QualityEvidenceService.QualityReportEvidence evidence =
                evidenceService.getReport(body.arguments().reportUid());
        String version = evidence.checkedAt() == null ? null : evidence.checkedAt().toString();
        return AgentContract.success(evidence, context, "XnetDataops/dqm", version, startedNanos);
    }

    /** 表示质量报告查询参数。 */
    public record QualityReportArguments(String reportUid) {
    }
}
