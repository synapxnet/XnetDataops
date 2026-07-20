package com.synapxnet.dataopsusrservice.controller;

import com.synapxnet.dataopsusrservice.common.Result;
import com.synapxnet.dataopsusrservice.entity.UserRole;
import com.synapxnet.dataopsusrservice.service.RoleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usr/user-roles")
public class UserRoleController {

    private final RoleService roleService;

    public UserRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public Result<UserRole> assign(@RequestBody UserRole userRole) {
        return Result.success(roleService.assignRole(userRole));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        roleService.removeRoleMapping(id);
        return Result.success();
    }
}
