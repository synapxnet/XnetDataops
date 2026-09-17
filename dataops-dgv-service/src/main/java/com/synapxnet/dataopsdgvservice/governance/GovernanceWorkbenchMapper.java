/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：读取有界治理事实。Purpose: Read bounded native governance facts.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.governance;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface GovernanceWorkbenchMapper {
    /** 只读已授权报告和持久化资产关联，排除任意明细JSON。 Read one granted report and persisted asset binding without arbitrary detail JSON. */
    @Select("SELECT r.uid, c.asset_uid, q.name AS rule_name, r.status, r.check_time AS checked_at, "
            + "r.total_rows, r.failed_rows, r.pass_rate FROM xnet_dataops_dqm_quality_report r "
            + "LEFT JOIN xnet_dataops_dqm_quality_rule q ON q.id=r.rule_id "
            + "JOIN xnet_dataops_dqm_contract_check c ON c.report_uid=r.uid "
            + "WHERE r.uid=#{uid} ORDER BY c.checked_at DESC LIMIT 1")
    @Options(timeout = 2)
    QualityRow quality(@Param("uid") String uid);

    /** 原生报告通过率单位为百分数，0.5表示0.5%。 Native passRate is a percent value: 0.5 means 0.5 percent. */
    record QualityRow(String uid, String assetUid, String ruleName, String status, LocalDateTime checkedAt,
                      Long totalRows, Long failedRows, BigDecimal passRate) { }
}
