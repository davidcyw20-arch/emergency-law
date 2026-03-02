package com.yunxian.emergencylaw.controller;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.StudyProgress;
import com.yunxian.emergencylaw.mapper.StudyProgressMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/learn")
@RequiredArgsConstructor
public class LearnController {

    private final StudyProgressMapper studyProgressMapper;

    /**
     * 上报/更新学习进度
     * 前端通常用 JSON body 提交：{courseId, lessonId, progressPercent, status}
     */
    @PostMapping("/my/progress/upsert")
    public ApiResponse<String> upsertProgress(@RequestBody ProgressUpsertReq req,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return ApiResponse.fail("Missing token");
        }

        if (req == null
                || req.getCourseId() == null
                || req.getLessonId() == null
                || req.getProgressPercent() == null
                || req.getStatus() == null) {
            return ApiResponse.fail("Missing params");
        }

        StudyProgress p = new StudyProgress();
        p.setUserId(userId);
        p.setCourseId(req.getCourseId());
        p.setLessonId(req.getLessonId());
        p.setProgressPercent(req.getProgressPercent());
        p.setStatus(req.getStatus());
        p.setLastStudyTime(LocalDateTime.now());

        // 自定义 upsert
        studyProgressMapper.upsert(p);

        return ApiResponse.ok("OK");
    }

    @Data
    public static class ProgressUpsertReq {
        private Long courseId;
        private Long lessonId;
        private Integer progressPercent;
        private String status;
    }

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
}
