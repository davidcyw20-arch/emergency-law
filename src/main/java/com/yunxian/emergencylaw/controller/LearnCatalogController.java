package com.yunxian.emergencylaw.controller;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.Course;
import com.yunxian.emergencylaw.entity.CourseCategory;
import com.yunxian.emergencylaw.entity.CourseLesson;
import com.yunxian.emergencylaw.service.LearnService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learn")
public class LearnCatalogController {

    private final LearnService learnService;

    public LearnCatalogController(LearnService learnService) {
        this.learnService = learnService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CourseCategory>> categories() {
        return ApiResponse.ok(learnService.listCategories());
    }

    @GetMapping("/courses")
    public ApiResponse<List<Course>> courses(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        return ApiResponse.ok(learnService.listCourses(categoryId));
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<Course> course(@PathVariable Long courseId) {
        Course course = learnService.getCourse(courseId);
        if (course == null) {
            return ApiResponse.fail("course not found");
        }
        return ApiResponse.ok(course);
    }

    @GetMapping("/course/{courseId}/lessons")
    public ApiResponse<List<CourseLesson>> lessons(@PathVariable Long courseId) {
        return ApiResponse.ok(learnService.listLessons(courseId));
    }

    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<CourseLesson> lesson(@PathVariable Long lessonId) {
        CourseLesson lesson = learnService.getLesson(lessonId);
        if (lesson == null) {
            return ApiResponse.fail("lesson not found");
        }
        return ApiResponse.ok(lesson);
    }
}
