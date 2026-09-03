package com.synapxnet.dataopsdsmservice.service;

import com.synapxnet.dataopsdsmservice.entity.DataSource;
import java.util.List;

public interface DataSourceService {
    /** 查询全部数据源。 */
    List<DataSource> listAll();

    /** 按类型查询数据源。 */
    List<DataSource> listByType(String type);

    /** 按主键查询数据源。 */
    DataSource getById(Long id);

    /** 创建数据源。 */
    DataSource create(DataSource dataSource);

    /** 更新数据源。 */
    DataSource update(DataSource dataSource);

    /** 删除数据源。 */
    void delete(Long id);

    /** 使用受控 JDBC 参数测试数据源连接。 */
    boolean testConnection(Long id);
}
