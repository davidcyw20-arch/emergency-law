package com.yunxian.emergencylaw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.SharePost;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SharePostMapper;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.service.ShareModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final SharePostMapper sharePostMapper;
    private final SysUserMapper sysUserMapper;
    private final ShareModerationService shareModerationService;

    /**
     * GET /api/share/posts
     * 经验分享列表
     */
    @GetMapping("/posts")
    public ApiResponse<List<PostVO>> list() {
        List<SharePost> list = sharePostMapper.selectList(
                new LambdaQueryWrapper<SharePost>()
                        .orderByDesc(SharePost::getCreatedAt)
        );

        List<SharePost> visible = list.stream()
                .filter(p -> !shareModerationService.isHidden(p.getId()))
                .collect(Collectors.toList());

        if (visible.isEmpty()) return ApiResponse.ok(new ArrayList<>());

        List<Long> userIds = visible.stream().map(SharePost::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds)
                ).stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<PostVO> out = new ArrayList<>();
        for (SharePost p : visible) {
            SysUser u = userMap.get(p.getUserId());
            PostVO vo = new PostVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setTitle(p.getTitle());
            vo.setContent(p.getContent());
            vo.setRegion(p.getRegion());
            vo.setLikes(p.getLikes());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setAuthor(u == null ? null : (u.getNickname() != null ? u.getNickname() : u.getUsername()));
            out.add(vo);
        }

        return ApiResponse.ok(out);
    }

    /**
     * POST /api/share/posts
     * 新增经验分享
     */
    @PostMapping("/posts")
    public ApiResponse<SharePost> create(@RequestBody CreateReq req,
                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ApiResponse.fail("Missing token");
        }

        SharePost p = new SharePost();
        p.setUserId(userId);
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setRegion(req.getRegion());
        p.setLikes(0);
        p.setCreatedAt(LocalDateTime.now());

        sharePostMapper.insert(p);
        return ApiResponse.ok(p);
    }

    /**
     * POST /api/share/post/{id}/like
     * 点赞
     */
    @PostMapping("/post/{id}/like")
    public ApiResponse<Integer> like(@PathVariable Long id) {
        int updated = sharePostMapper.update(
                null,
                new LambdaUpdateWrapper<SharePost>()
                        .eq(SharePost::getId, id)
                        .setSql("likes = IFNULL(likes, 0) + 1")
        );
        if (updated <= 0) {
            return ApiResponse.fail("Not found");
        }

        SharePost p = sharePostMapper.selectById(id);
        Integer likes = p == null || p.getLikes() == null ? 0 : p.getLikes();
        return ApiResponse.ok(likes);
    }

    /**
     * 从拦截器中取 userId
     */
    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建分享请求体（普通内部类，避免 Lombok 语法问题）
     */
    public static class CreateReq {
        private String title;
        private String content;
        private String region;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    public static class PostVO {
        private Long id;
        private Long userId;
        private String title;
        private String content;
        private String region;
        private Integer likes;
        private LocalDateTime createdAt;
        private String author;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public Integer getLikes() { return likes; }
        public void setLikes(Integer likes) { this.likes = likes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }
}
