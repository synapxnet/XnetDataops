package com.synapxnet.dataopsdsmservice.service.impl;

import com.synapxnet.dataopsdsmservice.entity.DataSource;
import com.synapxnet.dataopsdsmservice.mapper.DataSourceMapper;
import com.synapxnet.dataopsdsmservice.service.DataSourceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceMapper dataSourceMapper;

    public DataSourceServiceImpl(DataSourceMapper dataSourceMapper) {
        this.dataSourceMapper = dataSourceMapper;
    }

    @Override
    public List<DataSource> listAll() {
        return dataSourceMapper.findAll();
    }

    @Override
    public List<DataSource> listByType(String type) {
        return dataSourceMapper.findByType(type);
    }

    @Override
    public DataSource getById(Long id) {
        DataSource ds = dataSourceMapper.findById(id);
        if (ds == null) {
            throw new IllegalArgumentException("DataSource not found: " + id);
        }
        return ds;
    }

    @Override
    public DataSource create(DataSource dataSource) {
        dataSource.setUid(UUID.randomUUID().toString());
        if (dataSource.getStatus() == null) {
            dataSource.setStatus("inactive");
        }
        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    @Override
    public DataSource update(DataSource dataSource) {
        dataSourceMapper.update(dataSource);
        return dataSourceMapper.findById(dataSource.getId());
    }

    @Override
    public void delete(Long id) {
        dataSourceMapper.deleteById(id);
    }

    @Override
    public boolean testConnection(Long id) {
        DataSource ds = getById(id);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                    ds.getHost(), ds.getPort(), ds.getDatabaseName());
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, ds.getUsername(), ds.getEncryptedPassword());
            conn.close();
            dataSourceMapper.updateStatus(id, "active");
            return true;
        } catch (Exception e) {
            dataSourceMapper.updateStatus(id, "error");
            return false;
        }
    }
}
