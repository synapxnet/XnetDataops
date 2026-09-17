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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 为质量证据读取持久化契约检查事实。
 * English: Read or verify persisted quality evidence; preserve absent data and disclose malformed details.
 */
@Mapper
public interface AgentQualityMapper {

    /**
     * 根据报告 UID 查询最近的契约检查。
     *
     * @param reportUid 质量报告 UID
     * @return 契约检查，不存在时返回 null
 * English: Query the most recent persisted contract check for this report UID.
 */
    @Select("SELECT * FROM xnet_dataops_dqm_contract_check WHERE report_uid = #{reportUid} "
            + "ORDER BY checked_at DESC, id DESC LIMIT 1")
    ContractCheck findContractCheck(@Param("reportUid") String reportUid);
}
