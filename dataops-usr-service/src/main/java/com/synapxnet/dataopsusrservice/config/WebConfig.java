package com.synapxnet.dataopsusrservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final ManagementAuthorizationInterceptor managementAuthorizationInterceptor;

    /**
     * 创建 Web 配置并注入管理接口权限拦截器。
     *
     * @param managementAuthorizationInterceptor 管理接口权限拦截器
     */
    public WebConfig(ManagementAuthorizationInterceptor managementAuthorizationInterceptor) {
        this.managementAuthorizationInterceptor = managementAuthorizationInterceptor;
    }

    /**
     * 配置平台前端访问 API 所需的跨域策略。
     *
     * @param registry Spring MVC 跨域注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 将用户、角色、用户角色关系和统计接口限制为管理员访问。
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(managementAuthorizationInterceptor)
                .addPathPatterns(
                        "/api/usr/users/**",
                        "/api/usr/roles/**",
                        "/api/usr/user-roles/**",
                        "/api/usr/stats/**"
                );
    }
}
