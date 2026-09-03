package com.synapxnet.dataopsdsmservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapxnet.dataopsdsmservice.entity.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataSourceConnectionResolverTest {

    /** 验证 PostgreSQL 使用环境变量密码和受控 SSL 参数生成连接规格。 */
    @Test
    void resolvesPostgresqlConnectionFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("XNET_DATASOURCE_RECOMMENDATION_PASSWORD", "test-password");
        DataSourceConnectionResolver resolver = new DataSourceConnectionResolver(
                new ObjectMapper(),
                environment,
                "127.0.0.1"
        );
        DataSource dataSource = createPostgresqlDataSource("127.0.0.1");

        DataSourceConnectionResolver.ConnectionSpec spec = resolver.resolve(dataSource);

        assertEquals("recommendation_reader", spec.username());
        assertEquals("test-password", spec.password());
        assertEquals(7, spec.timeoutSeconds());
        assertEquals(
                "jdbc:postgresql://127.0.0.1:5432/recommendation?sslmode=require&connectTimeout=7&ApplicationName=XnetDataOps",
                spec.jdbcUrl()
        );
    }

    /** 验证连接地址不能通过 JDBC 查询参数注入绕过配置白名单。 */
    @Test
    void rejectsUnsafeHost() {
        DataSourceConnectionResolver resolver = new DataSourceConnectionResolver(
                new ObjectMapper(),
                new MockEnvironment(),
                ""
        );
        DataSource dataSource = createPostgresqlDataSource("127.0.0.1?sslmode=disable");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(dataSource));
    }

    /** 验证数据源序列化结果不会回传数据库密码。 */
    @Test
    void doesNotSerializePassword() throws Exception {
        DataSource dataSource = createPostgresqlDataSource("127.0.0.1");
        dataSource.setEncryptedPassword("must-not-leak");

        String json = new ObjectMapper().writeValueAsString(dataSource);

        assertFalse(json.contains("must-not-leak"));
        assertFalse(json.contains("encryptedPassword"));
    }

    /** 创建供连接策略测试使用的 PostgreSQL 数据源实体。 */
    private DataSource createPostgresqlDataSource(String host) {
        DataSource dataSource = new DataSource();
        dataSource.setType("POSTGRESQL");
        dataSource.setHost(host);
        dataSource.setPort(5432);
        dataSource.setDatabaseName("recommendation");
        dataSource.setUsername("recommendation_reader");
        dataSource.setConnectionParams(
                "{\"passwordEnv\":\"XNET_DATASOURCE_RECOMMENDATION_PASSWORD\",\"sslMode\":\"require\",\"connectTimeoutSeconds\":7}"
        );
        return dataSource;
    }
}
