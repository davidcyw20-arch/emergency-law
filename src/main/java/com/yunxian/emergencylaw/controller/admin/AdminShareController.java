package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.SharePost;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SharePostMapper;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.service.ShareModerationService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/share")
public class AdminShareController {

    private final SharePostMapper sharePostMapper;
    private final SysUserMapper sysUserMapper;
    private final ShareModerationService shareModerationService;

    public AdminShareController(SharePostMapper sharePostMapper, SysUserMapper sysUserMapper, ShareModerationService shareModerationService) {
        this.sharePostMapper = sharePostMapper;
        this.sysUserMapper = sysUserMapper;
        this.shareModerationService = shareModerationService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<PostVO>> list(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");

        List<SharePost> list = sharePostMapper.selectList(
                new LambdaQueryWrapper<SharePost>().orderByDesc(SharePost::getCreatedAt)
        );
        if (list.isEmpty()) return ApiResponse.ok(new ArrayList<>());

        List<Long> userIds = list.stream().map(SharePost::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds)
        ).stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<PostVO> out = new ArrayList<>();
        for (SharePost p : list) {
            SysUser u = userMap.get(p.getUserId());
            PostVO vo = new PostVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setRegion(p.getRegion());
            vo.setLikes(p.getLikes());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setAuthor(u == null ? null : (u.getNickname() != null ? u.getNickname() : u.getUsername()));
            boolean hidden = shareModerationService.isHidden(p.getId());
            vo.setStatus(hidden ? "已驳回/隐藏" : "已通过/展示");
            vo.setStatusKey(hidden ? "rejected" : "approved");
            out.add(vo);
        }

        return ApiResponse.ok(out);
    }

    @PostMapping("/post/{id}/approve")
    public ApiResponse<String> approve(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        SharePost p = sharePostMapper.selectById(id);
        if (p == null) return ApiResponse.fail("not found");
        shareModerationService.show(id);
        return ApiResponse.ok("approved");
    }

    @PostMapping("/post/{id}/reject")
    public ApiResponse<String> reject(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        SharePost p = sharePostMapper.selectById(id);
        if (p == null) return ApiResponse.fail("not found");
        shareModerationService.hide(id);
        return ApiResponse.ok("rejected");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    @Data
    public static class PostVO {
        private Long id;
        private String title;
        private String author;
        private String region;
        private Integer likes;
        private LocalDateTime createdAt;
        private String status;
        private String statusKey;
    }
}

