/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：验证网页HTTP错误包络。Purpose: Verify native browser HTTP error contracts.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.governance;

import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.DataOpsOrganizationVerifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GovernanceWorkbenchControllerTest {
    /** 网页鉴权失败保留原生code/message及HTTP状态，且不触发领域读取。 Browser authorization failures retain native code/message and HTTP status without domain reads. */
    @ParameterizedTest
    @ValueSource(ints = {401, 403, 503})
    void preservesNativeErrorEnvelope(int statusCode) throws Exception {
        var verifier = mock(DataOpsOrganizationVerifier.class);
        var service = mock(GovernanceWorkbenchService.class);
        when(verifier.verify(null, null, null, null)).thenThrow(new AgentContractException(statusCode, "SCOPE_REJECTED", "范围校验失败"));
        // 同时挂载真实公共Advice，验证控制器局部错误包络优先。 Register the actual shared advice to verify that the controller-local envelope takes precedence.
        var adviceConstructor = Class.forName("com.synapxnet.goai.contract.AgentExceptionHandler").getDeclaredConstructor();
        adviceConstructor.setAccessible(true);
        var mvc = MockMvcBuilders.standaloneSetup(new GovernanceWorkbenchController(verifier, service))
                .setControllerAdvice(adviceConstructor.newInstance()).build();
        mvc.perform(get("/api/dgv/governance/workbench"))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.code").value(statusCode))
                .andExpect(jsonPath("$.message").value("范围校验失败"))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.meta").doesNotExist());
        verifyNoInteractions(service);
    }
}
