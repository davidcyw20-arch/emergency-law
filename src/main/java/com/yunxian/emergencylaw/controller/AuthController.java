package com.yunxian.emergencylaw.controller;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.dto.LoginRequest;
import com.yunxian.emergencylaw.dto.LoginResponse;
import com.yunxian.emergencylaw.dto.RegisterRequest;
import com.yunxian.emergencylaw.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 探针：确认 controller 正常
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.okMsg("auth controller ok", "pong");
    }

    // 注册：写入数据库
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest req) {
        try {
            authService.register(req);
            return ApiResponse.okMsg("register success", "OK");
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    // 登录：返回 token
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest req) {
        try {
            return ApiResponse.ok(authService.login(req));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
