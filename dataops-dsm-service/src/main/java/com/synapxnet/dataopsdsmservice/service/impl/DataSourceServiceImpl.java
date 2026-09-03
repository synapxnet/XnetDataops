package com.synapxnet.dataopsdsmservice.service.impl;

import com.synapxnet.dataopsdsmservice.entity.DataSource;
import com.synapxnet.dataopsdsmservice.mapper.DataSourceMapper;
import com.synapxnet.dataopsdsmservice.service.DataSourceConnectionResolver;
import com.synapxnet.dataopsdsmservice.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

@Service
public class DataSourceServiceImpl implements DataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceServiceImpl.class);

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceConnectionResolver connectionResolver;

    /** 注入数据源持久层和受控连接参数解析器。 */
    public DataSourceServiceImpl(
            DataSourceMapper dataSourceMapper,
            DataSourceConnectionResolver connectionResolver
    ) {
        this.dataSourceMapper = dataSourceMapper;
        this.connectionResolver = connectionResolver;
    }

    /** 查询全部数据源。 */
    @Override
    public List<DataSource> listAll() {
        return dataSourceMapper.findAll();
    }

    /** 按类型查询数据源。 */
    @Override
    public List<DataSource> listByType(String type) {
        return dataSourceMapper.findByType(type);
    }

    /** 按主键查询数据源，不存在时返回业务参数错误。 */
    @Override
    public DataSource getById(Long id) {
        DataSource ds = dataSourceMapper.findById(id);
        if (ds == null) {
            throw new IllegalArgumentException("DataSource not found: " + id);
        }
        return ds;
    }

    /** 创建具有唯一标识和默认停用状态的数据源。 */
    @Override
    public DataSource create(DataSource dataSource) {
        dataSource.setUid(UUID.randomUUID().toString());
        if (dataSource.getStatus() == null) {
            dataSource.setStatus("inactive");
        }
        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    /** 更新数据源并返回最新配置。 */
    @Override
    public DataSource update(DataSource dataSource) {
        dataSourceMapper.update(dataSource);
        return dataSourceMapper.findById(dataSource.getId());
    }

    /** 按主键删除数据源。 */
    @Override
    public void delete(Long id) {
        dataSourceMapper.deleteById(id);
    }

    /** 使用数据库类型白名单和受控参数测试真实 JDBC 连接。 */
    @Override
    public boolean testConnection(Long id) {
        DataSource dataSource = getById(id);
        try {
            DataSourceConnectionResolver.ConnectionSpec connectionSpec = connectionResolver.resolve(dataSource);
            DriverManager.setLoginTimeout(connectionSpec.timeoutSeconds());
            try (Connection ignored = DriverManager.getConnection(
                    connectionSpec.jdbcUrl(),
                    connectionSpec.username(),
                    connectionSpec.password()
            )) {
                dataSourceMapper.updateStatus(id, "active");
            }
            return true;
        } catch (Exception exception) {
            log.warn(
                    "Data source connection test failed: id={}, type={}, reason={}",
                    id,
                    dataSource.getType(),
                    exception.getMessage()
            );
            dataSourceMapper.updateStatus(id, "error");
            return false;
        }
    }
}
