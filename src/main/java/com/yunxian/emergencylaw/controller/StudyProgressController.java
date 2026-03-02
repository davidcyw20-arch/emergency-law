package com.yunxian.emergencylaw.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.StudyProgress;
import com.yunxian.emergencylaw.mapper.StudyProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/learn/my/progress")
@RequiredArgsConstructor
public class StudyProgressController {

    private final StudyProgressMapper studyProgressMapper;

    /**
     * ✅ 前端正在请求的：GET /api/learn/my/progress
     */
    @GetMapping
    public ApiResponse<List<StudyProgress>> list(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");

        List<StudyProgress> list = studyProgressMapper.selectList(
                new LambdaQueryWrapper<StudyProgress>()
                        .eq(StudyProgress::getUserId, userId)
                        .orderByDesc(StudyProgress::getLastStudyTime)
        );
        return ApiResponse.ok(list);
    }

    /**
     * ✅ 课程进度：GET /api/learn/my/progress/{courseId}
     */
    @GetMapping("/{courseId}")
    public ApiResponse<StudyProgress> one(@PathVariable Long courseId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");

        StudyProgress one = studyProgressMapper.selectOne(
                new LambdaQueryWrapper<StudyProgress>()
                        .eq(StudyProgress::getUserId, userId)
                        .eq(StudyProgress::getCourseId, courseId)
                        .last("LIMIT 1")
        );
        return ApiResponse.ok(one);
    }

    /**
     * 从拦截器中取 userId
     */
    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
