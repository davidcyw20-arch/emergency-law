package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.Course;
import com.yunxian.emergencylaw.mapper.CourseMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CourseMapper courseMapper;

    public AdminCourseController(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @GetMapping
    public ApiResponse<List<Course>> list() {
        List<Course> list = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByDesc(Course::getId));
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<Course> create(@RequestBody CourseReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            return ApiResponse.fail("title required");
        }
        if (req.getCategoryId() == null) {
            return ApiResponse.fail("categoryId required");
        }
        Course c = new Course();
        c.setTitle(req.getTitle().trim());
        c.setCategoryId(req.getCategoryId());
        c.setSummary(req.getSummary());
        c.setCoverUrl(req.getCoverUrl());
        c.setDifficulty(req.getDifficulty());
        c.setEnabled(req.getEnabled() == null ? 0 : req.getEnabled());
        c.setSortNo(req.getSortNo());
        c.setCreateTime(LocalDateTime.now());
        courseMapper.insert(c);
        return ApiResponse.ok(c);
    }

    @PutMapping("/{id}")
    public ApiResponse<Course> update(@PathVariable Long id, @RequestBody CourseReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        Course c = courseMapper.selectById(id);
        if (c == null) return ApiResponse.fail("not found");
        if (req.getTitle() != null) c.setTitle(req.getTitle().trim());
        if (req.getCategoryId() != null) c.setCategoryId(req.getCategoryId());
        c.setSummary(req.getSummary());
        c.setCoverUrl(req.getCoverUrl());
        c.setDifficulty(req.getDifficulty());
        if (req.getSortNo() != null) c.setSortNo(req.getSortNo());
        if (req.getEnabled() != null) c.setEnabled(req.getEnabled());
        courseMapper.updateById(c);
        return ApiResponse.ok(c);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<String> publish(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        Course c = courseMapper.selectById(id);
        if (c == null) return ApiResponse.fail("not found");
        c.setEnabled(1);
        courseMapper.updateById(c);
        return ApiResponse.ok("published");
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<String> archive(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        Course c = courseMapper.selectById(id);
        if (c == null) return ApiResponse.fail("not found");
        c.setEnabled(0);
        courseMapper.updateById(c);
        return ApiResponse.ok("archived");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    @Data
    public static class CourseReq {
        private Long categoryId;
        private String title;
        private String summary;
        private String coverUrl;
        private String difficulty;
        private Integer enabled;
        private Integer sortNo;
    }
}
