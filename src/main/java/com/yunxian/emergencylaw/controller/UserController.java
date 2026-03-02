package com.yunxian.emergencylaw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final SysUserMapper sysUserMapper;

    public UserController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> me() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            return ApiResponse.fail("Not logged in");
        }

        SysUser u = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getId, userId)
        );
        if (u != null) {
            u.setPassword(null); // 不返回密码
        }
        return ApiResponse.ok(u);
    }
}
