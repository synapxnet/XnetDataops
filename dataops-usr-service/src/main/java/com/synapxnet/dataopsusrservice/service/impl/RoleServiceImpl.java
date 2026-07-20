package com.synapxnet.dataopsusrservice.service.impl;

import com.synapxnet.dataopsusrservice.entity.Role;
import com.synapxnet.dataopsusrservice.entity.UserRole;
import com.synapxnet.dataopsusrservice.mapper.RoleMapper;
import com.synapxnet.dataopsusrservice.mapper.UserRoleMapper;
import com.synapxnet.dataopsusrservice.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleServiceImpl(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<Role> listAll() {
        return roleMapper.findAll();
    }

    @Override
    public Role getById(Long id) {
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + id);
        }
        return role;
    }

    @Override
    public Role create(Role role) {
        Role existing = roleMapper.findByRoleCode(role.getRoleCode());
        if (existing != null) {
            throw new IllegalArgumentException("Role code already exists: " + role.getRoleCode());
        }
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role update(Role role) {
        roleMapper.update(role);
        return roleMapper.findById(role.getId());
    }

    @Override
    public void delete(Long id) {
        userRoleMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    @Override
    public List<UserRole> getRoleUsers(Long roleId) {
        return userRoleMapper.findByRoleId(roleId);
    }

    @Override
    public UserRole assignRole(UserRole userRole) {
        userRoleMapper.insert(userRole);
        return userRole;
    }

    @Override
    public void removeRoleMapping(Long id) {
        userRoleMapper.deleteById(id);
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        return userRoleMapper.findRoleCodesByUserId(userId);
    }
}
