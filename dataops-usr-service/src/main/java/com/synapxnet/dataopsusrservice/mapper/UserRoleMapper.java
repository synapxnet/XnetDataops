package com.synapxnet.dataopsusrservice.mapper;

import com.synapxnet.dataopsusrservice.entity.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Select("SELECT ur.*, u.username, r.role_name, r.role_code " +
            "FROM xnet_dataops_usr_user_role ur " +
            "LEFT JOIN xnet_dataops_usr_user u ON ur.user_id = u.id " +
            "LEFT JOIN xnet_dataops_usr_role r ON ur.role_id = r.id " +
            "WHERE ur.role_id = #{roleId}")
    List<UserRole> findByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT ur.*, u.username, r.role_name, r.role_code " +
            "FROM xnet_dataops_usr_user_role ur " +
            "LEFT JOIN xnet_dataops_usr_user u ON ur.user_id = u.id " +
            "LEFT JOIN xnet_dataops_usr_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<UserRole> findByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_code FROM xnet_dataops_usr_user_role ur " +
            "JOIN xnet_dataops_usr_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO xnet_dataops_usr_user_role (user_id, role_id, cluster_id) " +
            "VALUES (#{userId}, #{roleId}, #{clusterId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRole userRole);

    @Delete("DELETE FROM xnet_dataops_usr_user_role WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM xnet_dataops_usr_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM xnet_dataops_usr_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
