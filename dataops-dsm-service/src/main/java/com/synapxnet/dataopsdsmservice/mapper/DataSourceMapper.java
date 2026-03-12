package com.synapxnet.dataopsdsmservice.mapper;

import com.synapxnet.dataopsdsmservice.entity.DataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataSourceMapper {

    @Select("SELECT * FROM xnet_dataops_dsm_datasource ORDER BY created_at DESC")
    List<DataSource> findAll();

    @Select("SELECT * FROM xnet_dataops_dsm_datasource WHERE id = #{id}")
    DataSource findById(@Param("id") Long id);

    @Insert("INSERT INTO xnet_dataops_dsm_datasource (uid, name, type, host, port, database_name, username, encrypted_password, connection_params, status, description, created_by) " +
            "VALUES (#{uid}, #{name}, #{type}, #{host}, #{port}, #{databaseName}, #{username}, #{encryptedPassword}, #{connectionParams}, #{status}, #{description}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DataSource dataSource);

    @Update("UPDATE xnet_dataops_dsm_datasource SET name=#{name}, type=#{type}, host=#{host}, port=#{port}, database_name=#{databaseName}, " +
            "username=#{username}, encrypted_password=#{encryptedPassword}, connection_params=#{connectionParams}, " +
            "description=#{description} WHERE id=#{id}")
    int update(DataSource dataSource);

    @Update("UPDATE xnet_dataops_dsm_datasource SET status=#{status}, last_test_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM xnet_dataops_dsm_datasource WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_dataops_dsm_datasource WHERE type = #{type} ORDER BY created_at DESC")
    List<DataSource> findByType(@Param("type") String type);
}
