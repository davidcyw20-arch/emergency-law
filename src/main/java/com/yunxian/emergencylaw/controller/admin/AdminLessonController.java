package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.CourseLesson;
import com.yunxian.emergencylaw.mapper.CourseLessonMapper;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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


    @PostMapping("/upload-video")
    public ApiResponse<String> uploadVideo(@PathVariable Long courseId,
                                           @RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (file == null || file.isEmpty()) return ApiResponse.fail("file required");

        String original = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!".mp4".equals(ext) && !".webm".equals(ext) && !".ogg".equals(ext) && !".m3u8".equals(ext)) {
            return ApiResponse.fail("仅支持 mp4/webm/ogg/m3u8");
        }

        try {
            Path dir = Paths.get("data", "uploads", "videos", String.valueOf(courseId));
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/videos/" + courseId + "/" + name;
            return ApiResponse.ok(url);
        } catch (Exception e) {
            return ApiResponse.fail("upload failed: " + e.getMessage());
        }
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
