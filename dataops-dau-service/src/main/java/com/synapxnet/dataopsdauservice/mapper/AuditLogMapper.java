package com.synapxnet.dataopsdauservice.mapper;

import com.synapxnet.dataopsdauservice.entity.AuditLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AuditLogMapper {

    @Select("SELECT * FROM xnet_dataops_dau_audit_log ORDER BY operate_at DESC")
    List<AuditLog> findAll();

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE id = #{id}")
    AuditLog findById(@Param("id") Long id);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE module = #{module} ORDER BY operate_at DESC")
    List<AuditLog> findByModule(@Param("module") String module);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE action = #{action} ORDER BY operate_at DESC")
    List<AuditLog> findByAction(@Param("action") String action);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE user_id = #{userId} ORDER BY operate_at DESC")
    List<AuditLog> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE module = #{module} AND action = #{action} ORDER BY operate_at DESC")
    List<AuditLog> findByModuleAndAction(@Param("module") String module, @Param("action") String action);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE module = #{module} AND user_id = #{userId} ORDER BY operate_at DESC")
    List<AuditLog> findByModuleAndUserId(@Param("module") String module, @Param("userId") Long userId);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE action = #{action} AND user_id = #{userId} ORDER BY operate_at DESC")
    List<AuditLog> findByActionAndUserId(@Param("action") String action, @Param("userId") Long userId);

    @Select("SELECT * FROM xnet_dataops_dau_audit_log WHERE module = #{module} AND action = #{action} AND user_id = #{userId} ORDER BY operate_at DESC")
    List<AuditLog> findByModuleAndActionAndUserId(@Param("module") String module, @Param("action") String action, @Param("userId") Long userId);

    @Insert("INSERT INTO xnet_dataops_dau_audit_log (uid, user_id, username, module, action, target_type, target_id, target_name, detail, ip_address, operate_at) " +
            "VALUES (#{uid}, #{userId}, #{username}, #{module}, #{action}, #{targetType}, #{targetId}, #{targetName}, #{detail}, #{ipAddress}, #{operateAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog auditLog);

    @Update("UPDATE xnet_dataops_dau_audit_log SET user_id=#{userId}, username=#{username}, module=#{module}, action=#{action}, " +
            "target_type=#{targetType}, target_id=#{targetId}, target_name=#{targetName}, detail=#{detail}, ip_address=#{ipAddress} WHERE id=#{id}")
    int update(AuditLog auditLog);

    @Delete("DELETE FROM xnet_dataops_dau_audit_log WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT module, COUNT(*) as cnt FROM xnet_dataops_dau_audit_log GROUP BY module ORDER BY cnt DESC")
    List<Map<String, Object>> countByModule();

    @Select("SELECT action, COUNT(*) as cnt FROM xnet_dataops_dau_audit_log GROUP BY action ORDER BY cnt DESC")
    List<Map<String, Object>> countByAction();

    @Select("SELECT COUNT(*) FROM xnet_dataops_dau_audit_log")
    long countTotal();
}
