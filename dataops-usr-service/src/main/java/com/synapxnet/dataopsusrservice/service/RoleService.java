package com.synapxnet.dataopsusrservice.service;

import com.synapxnet.dataopsusrservice.entity.Role;
import com.synapxnet.dataopsusrservice.entity.UserRole;

import java.util.List;

public interface RoleService {

    List<Role> listAll();

    Role getById(Long id);

    Role create(Role role);

    Role update(Role role);

    void delete(Long id);

    List<UserRole> getRoleUsers(Long roleId);

    UserRole assignRole(UserRole userRole);

    void removeRoleMapping(Long id);

    List<String> getUserRoleCodes(Long userId);
}
