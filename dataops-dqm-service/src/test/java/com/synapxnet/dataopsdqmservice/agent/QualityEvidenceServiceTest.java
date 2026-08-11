package com.synapxnet.dataopsdqmservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.mapper.QualityRuleMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证质量报告比例归一化、契约事实优先级和坏 JSON 降级。
 */
class QualityEvidenceServiceTest {

    /** 120/128 契约事实必须来自持久记录，坏明细不得覆盖它。 */
    @Test
    void preservesContractFactsWhenDetailJsonIsInvalid() {
        QualityRuleMapper qualityMapper = mock(QualityRuleMapper.class);
        AgentQualityMapper agentMapper = mock(AgentQualityMapper.class);
        QualityReport report = new QualityReport();
        report.setId(7L);
        report.setUid("qr_risk_features_120");
        report.setRuleId(9L);
        report.setCheckTime(LocalDateTime.of(2026, 8, 2, 10, 0));
        report.setStatus("failed");
        report.setTotalRows(1_200_000L);
        report.setFailedRows(216_000L);
        report.setPassRate(new BigDecimal("82.00"));
        report.setDetailJson("{invalid-json");
        ContractCheck check = new ContractCheck();
        check.setUid("contract_check_risk_120_128");
        check.setReportUid(report.getUid());
        check.setAssetUid("asset_risk_features_prod");
        check.setSchemaSnapshotUid("schema_risk_features_120");
        check.setContractRef("contract_risk_v18");
        check.setExpectedFieldCount(128);
        check.setActualFieldCount(120);
        check.setStatus("FAILED");
        check.setCheckedAt(report.getCheckTime());
        when(qualityMapper.findReportByUid(report.getUid())).thenReturn(report);
        when(qualityMapper.findAlertsByReportId(report.getId())).thenReturn(List.of());
        when(agentMapper.findContractCheck(report.getUid())).thenReturn(check);
        QualityEvidenceService service = new QualityEvidenceService(
                qualityMapper, agentMapper, new ObjectMapper());

        QualityEvidenceService.QualityReportEvidence result = service.getReport(report.getUid());

        assertEquals(new BigDecimal("0.820000"), result.passRate());
        assertEquals(128, result.contractCheck().expectedFieldCount());
        assertEquals(120, result.contractCheck().actualFieldCount());
        assertTrue(result.warnings().contains("REPORT_DETAIL_INVALID_JSON"));
    }

    /** 固定量化报告缺失时必须返回带来源标记的完整性沙盘证据。 */
    @Test
    void returnsWhitelistedQuantitativeQualitySnapshot() {
        QualityRuleMapper qualityMapper = mock(QualityRuleMapper.class);
        AgentQualityMapper agentMapper = mock(AgentQualityMapper.class);
        QualityEvidenceService service = new QualityEvidenceService(
                qualityMapper, agentMapper, new ObjectMapper());

        QualityEvidenceService.QualityReportEvidence result = service.getReport(
                "report_quant_attribution_close");

        assertEquals("PASSED", result.qualityStatus());
        assertEquals(BigDecimal.ONE, result.passRate());
        assertEquals("asset_market_features_eod", result.assetUid());
        assertTrue(result.warnings().contains("COMPETITION_SANDBOX_SNAPSHOT"));
        verify(qualityMapper).findReportByUid("report_quant_attribution_close");
        verifyNoMoreInteractions(qualityMapper, agentMapper);
    }

    /** 未列入比赛白名单的缺失报告必须继续返回契约异常。 */
    @Test
    void rejectsUnknownMissingQualityReport() {
        QualityRuleMapper qualityMapper = mock(QualityRuleMapper.class);
        AgentQualityMapper agentMapper = mock(AgentQualityMapper.class);
        QualityEvidenceService service = new QualityEvidenceService(
                qualityMapper, agentMapper, new ObjectMapper());

        assertThrows(com.synapxnet.goai.contract.AgentContractException.class,
                () -> service.getReport("report_unknown"));
        verify(qualityMapper).findReportByUid("report_unknown");
        verifyNoMoreInteractions(qualityMapper, agentMapper);
    }
}
