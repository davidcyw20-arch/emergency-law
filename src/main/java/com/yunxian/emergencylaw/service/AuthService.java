package com.yunxian.emergencylaw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.dto.LoginRequest;
import com.yunxian.emergencylaw.dto.LoginResponse;
import com.yunxian.emergencylaw.dto.RegisterRequest;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.security.JwtUtil;
import com.yunxian.emergencylaw.security.PasswordMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final UserDisableService userDisableService;

    public AuthService(SysUserMapper sysUserMapper, JwtUtil jwtUtil, UserDisableService userDisableService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
        this.userDisableService = userDisableService;
    }

    public void register(RegisterRequest req) {
        if (req == null || !StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new RuntimeException("username/password required");
        }

        String username = req.getUsername().trim();
        String password = req.getPassword().trim();

        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            throw new RuntimeException("username already exists");
        }

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(password); // 先明文（后续我们再加密）
        u.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : username);
        u.setRegion(StringUtils.hasText(req.getRegion()) ? req.getRegion().trim() : null);
        u.setRole("USER");
        u.setCreateTime(LocalDateTime.now());

        sysUserMapper.insert(u);
    }

    public LoginResponse login(LoginRequest req) {
        if (req == null || !StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new RuntimeException("username/password required");
        }

        String username = req.getUsername().trim();
        String password = req.getPassword().trim();

        SysUser u = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (u == null) {
            throw new RuntimeException("user not found");
        }
        if (userDisableService.isDisabled(u.getId())) {
            throw new RuntimeException("账号已停用");
        }
        if (!PasswordMatcher.matches(password, u.getPassword())) {
            throw new RuntimeException("password incorrect");
        }

        String token = jwtUtil.generateToken(u.getId(), u.getUsername(), u.getRole());

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(u.getId());
        resp.setUsername(u.getUsername());
        resp.setNickname(u.getNickname());
        resp.setRole(u.getRole());
        return resp;
    }
}
