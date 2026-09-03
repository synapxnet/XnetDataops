package com.synapxnet.dataopstskservice.recommendation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;

/** 校验推荐数据产品写操作的登录令牌和 DataOps 角色。 */
@Component
public class RecommendationWriteAuthorizer {

    private static final String ACCESS_SQL = """
            SELECT COUNT(*)
            FROM xnet_dataops_usr_user AS user_account
            LEFT JOIN xnet_dataops_usr_user_role AS user_role
                ON user_role.user_id = user_account.id
            LEFT JOIN xnet_dataops_usr_role AS role
                ON role.id = user_role.role_id
            WHERE user_account.phone = ?
              AND user_account.status = 'active'
              AND (
                  LOWER(user_account.user_type) = 'admin'
                  OR role.role_code IN ('ADMIN', 'DEVELOPER')
              )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String jwtSecret;

    /** 注入平台元数据库和与登录服务一致的 JWT 密钥。 */
    public RecommendationWriteAuthorizer(
            DataSource dataSource,
            @Value("${JWT_SECRET:}") String jwtSecret
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jwtSecret = jwtSecret;
    }

    /** 验证 Bearer JWT，并确认账号具有数据产品写入角色。 */
    public String authorize(String authorization) {
        String phone = parsePhone(authorization);
        Integer matchCount = jdbcTemplate.queryForObject(ACCESS_SQL, Integer.class, phone);
        if (matchCount == null || matchCount < 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有数据产品写入权限");
        }
        return phone;
    }

    /** 从 Authorization 请求头解析已签名且未过期的账号手机号。 */
    private String parsePhone(String authorization) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "需要有效的登录令牌");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
            String phone = claims.getSubject();
            if (phone == null || phone.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌缺少账号标识");
            }
            return phone;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录令牌无效或已过期");
        }
    }
}
