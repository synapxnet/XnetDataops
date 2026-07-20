package com.synapxnet.dataopsdsmservice.service;

import com.synapxnet.dataopsdsmservice.entity.DataSource;
import java.util.List;

public interface DataSourceService {
    List<DataSource> listAll();
    List<DataSource> listByType(String type);
    DataSource getById(Long id);
    DataSource create(DataSource dataSource);
    DataSource update(DataSource dataSource);
    void delete(Long id);
    boolean testConnection(Long id);
}
