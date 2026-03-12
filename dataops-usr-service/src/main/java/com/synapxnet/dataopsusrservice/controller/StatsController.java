package com.synapxnet.dataopsusrservice.controller;

import com.synapxnet.dataopsusrservice.common.Result;
import com.synapxnet.dataopsusrservice.mapper.RoleMapper;
import com.synapxnet.dataopsusrservice.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usr/stats")
public class StatsController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public StatsController(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.count());
        stats.put("roleCount", roleMapper.count());
        return Result.success(stats);
    }
}
