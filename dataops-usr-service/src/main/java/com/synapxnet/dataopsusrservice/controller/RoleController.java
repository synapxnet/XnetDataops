package com.synapxnet.dataopsusrservice.controller;

import com.synapxnet.dataopsusrservice.common.Result;
import com.synapxnet.dataopsusrservice.entity.Role;
import com.synapxnet.dataopsusrservice.entity.UserRole;
import com.synapxnet.dataopsusrservice.service.RoleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usr/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public Result<List<Role>> list() {
        return Result.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Role> get(@PathVariable("id") Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    public Result<Role> create(@RequestBody Role role) {
        return Result.success(roleService.create(role));
    }

    @PutMapping("/{id}")
    public Result<Role> update(@PathVariable("id") Long id, @RequestBody Role role) {
        role.setId(id);
        return Result.success(roleService.update(role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @GetMapping("/{roleId}/users")
    public Result<List<UserRole>> getRoleUsers(@PathVariable("roleId") Long roleId) {
        return Result.success(roleService.getRoleUsers(roleId));
    }
}
