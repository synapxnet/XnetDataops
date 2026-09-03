package com.synapxnet.dataopsdsmservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdsmservice.entity.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DataSourceConnectionResolver {

    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]+$");
    private static final Pattern DATABASE_PATTERN = Pattern.compile("^[A-Za-z0-9_$-]+$");
    private static final Pattern PASSWORD_ENV_PATTERN = Pattern.compile("^XNET_DATASOURCE_[A-Z0-9_]+$");
    private static final Set<String> POSTGRESQL_SSL_MODES = Set.of(
            "disable", "allow", "prefer", "require", "verify-ca", "verify-full"
    );

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final Set<String> allowedHosts;

    /** 创建连接参数解析器并加载可选的数据源主机白名单。 */
    public DataSourceConnectionResolver(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${xnet.datasource.allowed-hosts:}") String allowedHosts
    ) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.allowedHosts = parseAllowedHosts(allowedHosts);
    }

    /** 将数据源实体转换为经过白名单校验的 JDBC 连接规格。 */
    public ConnectionSpec resolve(DataSource dataSource) {
        JsonNode params = parseConnectionParams(dataSource.getConnectionParams());
        String type = normalizeType(dataSource.getType());
        validateAddress(dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
        String password = resolvePassword(dataSource, params, environment::getProperty);
        int timeoutSeconds = readTimeoutSeconds(params);
        String jdbcUrl = buildJdbcUrl(type, dataSource, params, timeoutSeconds);
        return new ConnectionSpec(jdbcUrl, dataSource.getUsername(), password, timeoutSeconds);
    }

    /** 解析逗号分隔的主机白名单并统一为小写。 */
    private Set<String> parseAllowedHosts(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 解析连接扩展参数，空值按空 JSON 对象处理。 */
    private JsonNode parseConnectionParams(String value) {
        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode params = objectMapper.readTree(value);
            if (!params.isObject()) {
                throw new IllegalArgumentException("connectionParams 必须是 JSON 对象");
            }
            return params;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("connectionParams 不是有效 JSON", exception);
        }
    }

    /** 规范数据库类型并拒绝当前未实现安全连接策略的类型。 */
    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MYSQL", "POSTGRESQL").contains(type)) {
            throw new IllegalArgumentException("连接测试仅支持 MYSQL 和 POSTGRESQL");
        }
        return type;
    }

    /** 校验主机、端口、数据库名及生产环境主机白名单。 */
    private void validateAddress(String host, Integer port, String databaseName) {
        if (host == null || !HOST_PATTERN.matcher(host).matches()) {
            throw new IllegalArgumentException("数据源主机格式不合法");
        }
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("数据源主机不在允许列表中");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("数据源端口必须位于 1 到 65535 之间");
        }
        if (databaseName == null || !DATABASE_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("数据库名称格式不合法");
        }
    }

    /** 优先从受限前缀环境变量读取密码，并兼容已有的数据库密码字段。 */
    private String resolvePassword(
            DataSource dataSource,
            JsonNode params,
            Function<String, String> environmentResolver
    ) {
        String passwordEnvironment = readText(params, "passwordEnv", "").trim();
        if (!passwordEnvironment.isEmpty()) {
            if (!PASSWORD_ENV_PATTERN.matcher(passwordEnvironment).matches()) {
                throw new IllegalArgumentException("passwordEnv 必须使用 XNET_DATASOURCE_ 前缀");
            }
            String password = environmentResolver.apply(passwordEnvironment);
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("数据源密码环境变量未配置");
            }
            return password;
        }
        if (dataSource.getEncryptedPassword() == null || dataSource.getEncryptedPassword().isBlank()) {
            throw new IllegalArgumentException("数据源密码未配置");
        }
        return dataSource.getEncryptedPassword();
    }

    /** 读取并限制 JDBC 连接超时时间。 */
    private int readTimeoutSeconds(JsonNode params) {
        int timeoutSeconds = params.path("connectTimeoutSeconds").asInt(5);
        if (timeoutSeconds < 1 || timeoutSeconds > 30) {
            throw new IllegalArgumentException("connectTimeoutSeconds 必须位于 1 到 30 之间");
        }
        return timeoutSeconds;
    }

    /** 根据数据库类型生成不接受任意 URL 参数注入的 JDBC 地址。 */
    private String buildJdbcUrl(
            String type,
            DataSource dataSource,
            JsonNode params,
            int timeoutSeconds
    ) {
        if ("POSTGRESQL".equals(type)) {
            String sslMode = readText(params, "sslMode", "prefer").toLowerCase(Locale.ROOT);
            if (!POSTGRESQL_SSL_MODES.contains(sslMode)) {
                throw new IllegalArgumentException("PostgreSQL sslMode 不在允许列表中");
            }
            return String.format(
                    "jdbc:postgresql://%s:%d/%s?sslmode=%s&connectTimeout=%d&ApplicationName=XnetDataOps",
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getDatabaseName(),
                    sslMode,
                    timeoutSeconds
            );
        }
        boolean useSsl = !"disable".equalsIgnoreCase(readText(params, "sslMode", "prefer"));
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&connectTimeout=%d",
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName(),
                useSsl,
                timeoutSeconds * 1000
        );
    }

    /** 读取 JSON 文本字段，并在字段缺失或为空节点时返回默认值。 */
    private String readText(JsonNode params, String fieldName, String defaultValue) {
        JsonNode value = params.get(fieldName);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }

    public record ConnectionSpec(String jdbcUrl, String username, String password, int timeoutSeconds) {
        /** 保存已校验的 JDBC 地址、账户、密码和连接超时。 */
        public ConnectionSpec {
            if (jdbcUrl == null || username == null || password == null) {
                throw new IllegalArgumentException("JDBC 连接规格不能为空");
            }
        }
    }
}
