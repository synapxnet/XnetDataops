package com.synapxnet.dataopsusrservice.config;

import com.synapxnet.dataopsusrservice.common.ForbiddenOperationException;
import com.synapxnet.dataopsusrservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ManagementAuthorizationInterceptorTest {
    private AuthService authService;
    private ManagementAuthorizationInterceptor interceptor;

    /**
     * 为每个测试创建独立的认证服务替身和权限拦截器。
     */
    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        interceptor = new ManagementAuthorizationInterceptor(authService);
    }

    /**
     * 验证跨域预检请求无需携带认证头即可通过。
     */
    @Test
    void shouldAllowCorsPreflightWithoutAuthentication() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/usr/roles");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verifyNoInteractions(authService);
    }

    /**
     * 验证缺少 Bearer Token 的管理请求被判定为未认证。
     */
    @Test
    void shouldRejectRequestWithoutBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usr/roles");

        assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    /**
     * 验证普通比赛开发者不能访问角色管理接口。
     */
    @Test
    void shouldRejectNonAdministrator() {
        MockHttpServletRequest request = authorizedRequest();
        when(authService.getUserInfo("test-token")).thenReturn(Map.of(
                "userType", "user",
                "roles", List.of("DEVELOPER")
        ));

        assertThrows(
                ForbiddenOperationException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    /**
     * 验证明确具有 ADMIN 角色的登录用户可以访问管理接口。
     */
    @Test
    void shouldAllowAdministratorRole() {
        MockHttpServletRequest request = authorizedRequest();
        when(authService.getUserInfo("test-token")).thenReturn(Map.of(
                "userType", "user",
                "roles", List.of("ADMIN")
        ));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    /**
     * 创建携带测试 Bearer Token 的管理请求。
     *
     * @return 已设置认证头的请求对象
     */
    private MockHttpServletRequest authorizedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usr/roles");
        request.addHeader("Authorization", "Bearer test-token");
        return request;
    }
}
