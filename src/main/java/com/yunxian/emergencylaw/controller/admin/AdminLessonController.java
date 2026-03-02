package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.CourseLesson;
import com.yunxian.emergencylaw.mapper.CourseLessonMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/courses/{courseId}/lessons")
public class AdminLessonController {

    private final CourseLessonMapper lessonMapper;

    public AdminLessonController(CourseLessonMapper lessonMapper) {
        this.lessonMapper = lessonMapper;
    }

    @GetMapping
    public ApiResponse<List<CourseLesson>> list(@PathVariable Long courseId) {
        List<CourseLesson> list = lessonMapper.selectList(new LambdaQueryWrapper<CourseLesson>()
                .eq(CourseLesson::getCourseId, courseId)
                .orderByAsc(CourseLesson::getSortNo)
                .orderByAsc(CourseLesson::getId));
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<CourseLesson> create(@PathVariable Long courseId, @RequestBody LessonReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            return ApiResponse.fail("title required");
        }
        CourseLesson l = new CourseLesson();
        l.setCourseId(courseId);
        l.setTitle(req.getTitle().trim());
        l.setContentType(req.getContentType());
        l.setContentText(req.getContentText());
        l.setContentUrl(req.getContentUrl());
        l.setDurationMin(req.getDurationMin());
        l.setSortNo(req.getSortNo());
        l.setEnabled(1);
        l.setCreateTime(LocalDateTime.now());
        lessonMapper.insert(l);
        return ApiResponse.ok(l);
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseLesson> update(@PathVariable Long courseId, @PathVariable Long id,
                                            @RequestBody LessonReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        CourseLesson l = lessonMapper.selectById(id);
        if (l == null || !courseId.equals(l.getCourseId())) return ApiResponse.fail("not found");
        if (req.getTitle() != null) l.setTitle(req.getTitle().trim());
        l.setContentType(req.getContentType());
        l.setContentText(req.getContentText());
        l.setContentUrl(req.getContentUrl());
        if (req.getDurationMin() != null) l.setDurationMin(req.getDurationMin());
        if (req.getSortNo() != null) l.setSortNo(req.getSortNo());
        lessonMapper.updateById(l);
        return ApiResponse.ok(l);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long courseId, @PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        CourseLesson l = lessonMapper.selectById(id);
        if (l == null || !courseId.equals(l.getCourseId())) return ApiResponse.fail("not found");
        lessonMapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    @Data
    public static class LessonReq {
        private String title;
        private String contentType; // TEXT/LINK/VIDEO
        private String contentText;
        private String contentUrl;
        private Integer durationMin;
        private Integer sortNo;
    }
}
