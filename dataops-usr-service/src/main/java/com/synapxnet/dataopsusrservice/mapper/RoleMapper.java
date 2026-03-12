package com.synapxnet.dataopsusrservice.mapper;

import com.synapxnet.dataopsusrservice.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM xnet_dataops_usr_role ORDER BY created_at DESC")
    List<Role> findAll();

    @Select("SELECT * FROM xnet_dataops_usr_role WHERE id = #{id}")
    Role findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_dataops_usr_role WHERE role_code = #{roleCode}")
    Role findByRoleCode(@Param("roleCode") String roleCode);

    @Insert("INSERT INTO xnet_dataops_usr_role (role_name, role_code, description) " +
            "VALUES (#{roleName}, #{roleCode}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);

    @Update("UPDATE xnet_dataops_usr_role SET role_name=#{roleName}, description=#{description} WHERE id=#{id}")
    int update(Role role);

    @Delete("DELETE FROM xnet_dataops_usr_role WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM xnet_dataops_usr_role")
    int count();
}
