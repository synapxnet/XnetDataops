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
package com.synapxnet.dataopsdqmservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdqmservice.entity.QualityReport;
import com.synapxnet.dataopsdqmservice.mapper.QualityRuleMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证质量报告比例归一化、契约事实优先级和坏 JSON 降级。
 * English: Read or verify persisted quality evidence; preserve absent data and disclose malformed details.
 */
class QualityEvidenceServiceTest {

    /** 120/128 契约事实必须来自持久记录，坏明细不得覆盖它。
 * English: Apply the documented typed evidence operation and preserve its explicit input, output and failure boundaries.
 */
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
}
