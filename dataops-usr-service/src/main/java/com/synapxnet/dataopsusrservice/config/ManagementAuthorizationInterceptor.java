package com.synapxnet.dataopsusrservice.config;

import com.synapxnet.dataopsusrservice.common.ForbiddenOperationException;
import com.synapxnet.dataopsusrservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;
import java.util.Map;

@Component
public class ManagementAuthorizationInterceptor implements HandlerInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_ERROR_MESSAGE = "Invalid or expired token";
    private static final String ADMIN_ROLE = "ADMIN";

    private final AuthService authService;

    /**
     * 创建管理接口权限拦截器。
     *
     * @param authService 登录身份和角色解析服务
     */
    public ManagementAuthorizationInterceptor(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 允许跨域预检请求，并要求其他管理请求具有管理员身份。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前请求处理器
     * @return 通过校验时返回 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Map<String, Object> userInfo = authService.getUserInfo(extractBearerToken(request));
        if (isAdministrator(userInfo)) {
            return true;
        }
        throw new ForbiddenOperationException("Administrator permission required");
    }

    /**
     * 从标准 Authorization 请求头中提取非空 Bearer Token。
     *
     * @param request 当前 HTTP 请求
     * @return 去除 Bearer 前缀后的 Token
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException(TOKEN_ERROR_MESSAGE);
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException(TOKEN_ERROR_MESSAGE);
        }
        return token;
    }

    /**
     * 判断用户信息是否明确包含管理员用户类型或管理员角色。
     *
     * @param userInfo 已认证用户信息
     * @return 具有管理员权限时返回 true
     */
    private boolean isAdministrator(Map<String, Object> userInfo) {
        if ("admin".equalsIgnoreCase(String.valueOf(userInfo.get("userType")))) {
            return true;
        }
        Object roles = userInfo.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return false;
        }
        return roleCollection.stream().anyMatch(role -> ADMIN_ROLE.equalsIgnoreCase(String.valueOf(role)));
    }
}
