package com.synapxnet.dataopsusrservice.controller;

import com.synapxnet.dataopsusrservice.common.Result;
import com.synapxnet.dataopsusrservice.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usr")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sms-code")
    public Result<Map<String, Object>> sendSmsCode(@RequestBody Map<String, String> params) {
        String userPhone = params.get("userPhone");
        return Result.success(authService.sendSmsCode(userPhone));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String userPhone = params.get("userPhone");
        String code = params.get("code");
        return Result.success(authService.login(userPhone, code));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            authService.logout(token);
        }
        return Result.success();
    }

    @GetMapping("/user/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Result.success(authService.getUserInfo(token));
    }

    @GetMapping("/auth/codes")
    public Result<List<String>> getAccessCodes(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Result.success(authService.getAccessCodes(token));
    }
}
