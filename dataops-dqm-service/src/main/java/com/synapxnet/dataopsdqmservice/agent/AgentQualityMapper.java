package com.synapxnet.dataopsdqmservice.agent;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 为质量证据读取持久化契约检查事实。
 */
@Mapper
public interface AgentQualityMapper {

    /**
     * 根据报告 UID 查询最近的契约检查。
     *
     * @param reportUid 质量报告 UID
     * @return 契约检查，不存在时返回 null
     */
    @Select("SELECT * FROM xnet_dataops_dqm_contract_check WHERE report_uid = #{reportUid} "
            + "ORDER BY checked_at DESC, id DESC LIMIT 1")
    ContractCheck findContractCheck(@Param("reportUid") String reportUid);
}
