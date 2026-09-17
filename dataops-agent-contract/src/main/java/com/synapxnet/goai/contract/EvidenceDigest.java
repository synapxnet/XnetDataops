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
package com.synapxnet.goai.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * 为结构化证据生成确定性的内容摘要和外部证据编号。
 * English: Build or verify a canonical evidence digest without treating timestamps as resource versions.
 */
final class EvidenceDigest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    /** 阻止摘要工具被实例化。
 * English: Prevent instantiation of this digest utility.
 */
    private EvidenceDigest() {
    }

    /**
     * 根据来源、资源版本、观测时间和结构化内容生成证据编号。
     *
     * @param source 证据来源
     * @param data 结构化证据
     * @param resourceVersion 资源版本
     * @param observedAt 观测时间
     * @return 以 ev_ 开头的证据编号
 * English: Compute an evidence identifier from the source, resource version, observation time and structured content.
 */
    static String create(String source, Object data, String resourceVersion, Instant observedAt) {
        try {
            String canonical = source + "\n" + resourceVersion + "\n" + observedAt + "\n"
                    + OBJECT_MAPPER.writeValueAsString(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String value = HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
            return "ev_" + value.substring(0, 26);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new AgentContractException(500, "INTERNAL_ERROR", "无法生成证据摘要");
        }
    }
}
