package com.synapxnet.dataopstskservice.agent;

import com.synapxnet.goai.contract.AgentContract;
import com.synapxnet.goai.contract.AgentContractException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 DataOps 工作流实例只读证据工具。
 */
@RestController
public class AgentWorkflowToolController {

    private static final String TOOL_NAME = "dataops.workflow.instance.get";
    private final WorkflowInstanceEvidenceService evidenceService;

    /**
     * 创建工作流实例工具 Controller。
     *
     * @param evidenceService 任务实例证据领域服务
     */
    public AgentWorkflowToolController(WorkflowInstanceEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    /**
     * 获取任务实例、节点状态和可选脱敏日志摘要。
     *
     * @param body 强类型工具请求
     * @param servletRequest 当前 HTTP 请求
     * @return 工作流实例证据
     */
    @PostMapping("/api/agent/v1/tools/dataops.workflow.instance.get:invoke")
    public AgentContract.ToolResponse<WorkflowInstanceEvidenceService.WorkflowInstanceEvidence> invoke(
            @RequestBody AgentContract.ToolRequest<WorkflowInstanceArguments> body,
            HttpServletRequest servletRequest) {
        long startedNanos = System.nanoTime();
        AgentContract.RequestContext context = AgentContract.requireContext(servletRequest, TOOL_NAME, body);
        if (body.arguments() == null) {
            throw new AgentContractException(400, "INVALID_ARGUMENT", "arguments 不能为空");
        }
        WorkflowInstanceEvidenceService.WorkflowInstanceEvidence evidence = evidenceService.get(
                body.arguments().instanceUid(), Boolean.TRUE.equals(body.arguments().includeLogSummary()));
        String version = evidence.completedAt() == null ? null : evidence.completedAt().toString();
        return AgentContract.success(evidence, context, "XnetDataops/tsk", version, startedNanos);
    }

    /** 表示工作流实例查询参数。 */
    public record WorkflowInstanceArguments(String instanceUid, Boolean includeLogSummary) {
    }
}
