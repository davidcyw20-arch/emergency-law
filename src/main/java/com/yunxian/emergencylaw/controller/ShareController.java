package com.yunxian.emergencylaw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.SharePost;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.SharePostMapper;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.service.ShareCommentService;
import com.yunxian.emergencylaw.service.ShareInteractionService;
import com.yunxian.emergencylaw.service.ShareModerationService;
import com.yunxian.emergencylaw.service.ShareTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final SharePostMapper sharePostMapper;
    private final SysUserMapper sysUserMapper;
    private final ShareModerationService shareModerationService;
    private final ShareTagService shareTagService;
    private final ShareInteractionService shareInteractionService;
    private final ShareCommentService shareCommentService;

    @GetMapping("/posts")
    public ApiResponse<List<PostVO>> list(HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        List<SharePost> list = sharePostMapper.selectList(
                new LambdaQueryWrapper<SharePost>().orderByDesc(SharePost::getCreatedAt)
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
            vo.setTags(shareTagService.getTags(p.getId()));
            vo.setCommentCount(shareCommentService.count(p.getId()));
            vo.setLikedByMe(currentUserId != null && shareInteractionService.hasLiked(p.getId(), currentUserId));
            out.add(vo);
        }

        return ApiResponse.ok(out);
    }

    @PostMapping("/posts")
    public ApiResponse<SharePost> create(@RequestBody CreateReq req, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");
        if (req == null || !StringUtils.hasText(req.getTitle()) || !StringUtils.hasText(req.getContent())) {
            return ApiResponse.fail("title/content required");
        }

        SharePost p = new SharePost();
        p.setUserId(userId);
        p.setTitle(req.getTitle().trim());
        p.setContent(req.getContent().trim());
        p.setRegion(req.getRegion());
        p.setLikes(0);
        p.setCreatedAt(LocalDateTime.now());

        sharePostMapper.insert(p);
        shareTagService.setTags(p.getId(), req.getTags());
        return ApiResponse.ok(p);
    }

    @PostMapping("/post/{id}/like")
    public ApiResponse<Integer> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");

        SharePost p = sharePostMapper.selectById(id);
        if (p == null) return ApiResponse.fail("Not found");

        boolean added = shareInteractionService.addLike(id, userId);
        if (!added) {
            Integer likes = p.getLikes() == null ? 0 : p.getLikes();
            return ApiResponse.fail("你已经点过赞了（不可重复点赞）");
        }

        sharePostMapper.update(
                null,
                new LambdaUpdateWrapper<SharePost>()
                        .eq(SharePost::getId, id)
                        .setSql("likes = IFNULL(likes, 0) + 1")
        );

        SharePost latest = sharePostMapper.selectById(id);
        Integer likes = latest == null || latest.getLikes() == null ? 0 : latest.getLikes();
        return ApiResponse.ok(likes);
    }

    @GetMapping("/post/{id}/comments")
    public ApiResponse<List<CommentVO>> comments(@PathVariable Long id) {
        SharePost p = sharePostMapper.selectById(id);
        if (p == null) return ApiResponse.fail("Not found");

        List<CommentVO> out = shareCommentService.list(id).stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setAuthor(c.getAuthor());
            vo.setContent(c.getContent());
            vo.setCreatedAt(c.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
        return ApiResponse.ok(out);
    }

    @PostMapping("/post/{id}/comment")
    public ApiResponse<CommentVO> comment(@PathVariable Long id, @RequestBody CommentReq req, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");
        if (req == null || !StringUtils.hasText(req.getContent())) return ApiResponse.fail("content required");

        SharePost p = sharePostMapper.selectById(id);
        if (p == null) return ApiResponse.fail("Not found");
        if (p.getUserId() != null && p.getUserId().equals(userId)) {
            return ApiResponse.fail("请让其他用户来评论（作者本人不可评论自己的帖子）");
        }

        SysUser u = sysUserMapper.selectById(userId);
        String author = u == null ? "匿名" : (StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        ShareCommentService.CommentRecord saved = shareCommentService.add(id, userId, author, req.getContent());

        CommentVO vo = new CommentVO();
        vo.setId(saved.getId());
        vo.setPostId(saved.getPostId());
        vo.setUserId(saved.getUserId());
        vo.setAuthor(saved.getAuthor());
        vo.setContent(saved.getContent());
        vo.setCreatedAt(saved.getCreatedAt());
        return ApiResponse.ok(vo);
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    public static class CreateReq {
        private String title;
        private String content;
        private String region;
        private List<String> tags;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class CommentReq {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class CommentVO {
        private Long id;
        private Long postId;
        private Long userId;
        private String author;
        private String content;
        private LocalDateTime createdAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
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
        private List<String> tags;
        private Integer commentCount;
        private Boolean likedByMe;
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
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Integer getCommentCount() { return commentCount; }
        public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
        public Boolean getLikedByMe() { return likedByMe; }
        public void setLikedByMe(Boolean likedByMe) { this.likedByMe = likedByMe; }
    }
}
