package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.service.UserDisableService;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final SysUserMapper sysUserMapper;
    private final UserDisableService userDisableService;

    public AdminUserController(SysUserMapper sysUserMapper, UserDisableService userDisableService) {
        this.sysUserMapper = sysUserMapper;
        this.userDisableService = userDisableService;
    }

    @GetMapping
    public ApiResponse<List<UserVO>> list(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        List<SysUser> list = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId)
        );
        if (list.isEmpty()) return ApiResponse.ok(new ArrayList<>());

        List<UserVO> out = new ArrayList<>();
        for (SysUser u : list) {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole());
            vo.setRegion(u.getRegion());
            vo.setCreateTime(u.getCreateTime());
            vo.setDisabled(userDisableService.isDisabled(u.getId()));
            out.add(vo);
        }
        return ApiResponse.ok(out);
    }

    @PostMapping("/{id}/role")
    public ApiResponse<String> updateRole(@PathVariable Long id, @RequestBody RoleReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (req == null || !StringUtils.hasText(req.getRole())) return ApiResponse.fail("role required");
        String role = req.getRole().trim().toUpperCase();
        if (!"ADMIN".equals(role) && !"USER".equals(role)) return ApiResponse.fail("role invalid");

        Long currentUserId = getUserId(request);
        if (currentUserId != null && currentUserId.equals(id) && !"ADMIN".equals(role)) {
            return ApiResponse.fail("不能修改自己的管理员身份");
        }

        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return ApiResponse.fail("not found");

        if ("USER".equals(role) && "ADMIN".equalsIgnoreCase(u.getRole())) {
            Long adminCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")
            );
            if (adminCount != null && adminCount <= 1) {
                return ApiResponse.fail("至少保留 1 个管理员账号");
            }
        }

        u.setRole(role);
        sysUserMapper.updateById(u);
        return ApiResponse.ok("updated");
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return ApiResponse.fail("not found");
        String pwd = (req != null && StringUtils.hasText(req.getPassword())) ? req.getPassword().trim() : "123456";
        u.setPassword(pwd);
        sysUserMapper.updateById(u);
        return ApiResponse.ok("reset");
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<String> disable(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return ApiResponse.fail("not found");

        Long currentUserId = getUserId(request);
        if (currentUserId != null && currentUserId.equals(id)) {
            return ApiResponse.fail("不能停用自己");
        }

        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            Long adminCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")
            );
            if (adminCount != null && adminCount <= 1) {
                return ApiResponse.fail("不能停用最后一个管理员账号");
            }
        }

        userDisableService.disable(id);
        return ApiResponse.ok("disabled");
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<String> enable(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return ApiResponse.fail("not found");
        userDisableService.enable(id);
        return ApiResponse.ok("enabled");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    @Data
    public static class RoleReq {
        private String role;
    }

    @Data
    public static class ResetPasswordReq {
        private String password;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String role;
        private String region;
        private LocalDateTime createTime;
        private boolean disabled;
    }
}
