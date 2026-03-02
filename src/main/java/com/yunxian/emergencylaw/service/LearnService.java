package com.yunxian.emergencylaw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.entity.*;
import com.yunxian.emergencylaw.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearnService {

    private final CourseCategoryMapper categoryMapper;
    private final CourseMapper courseMapper;
    private final CourseLessonMapper lessonMapper;
    private final StudyProgressMapper progressMapper;

    public LearnService(CourseCategoryMapper categoryMapper,
                        CourseMapper courseMapper,
                        CourseLessonMapper lessonMapper,
                        StudyProgressMapper progressMapper) {
        this.categoryMapper = categoryMapper;
        this.courseMapper = courseMapper;
        this.lessonMapper = lessonMapper;
        this.progressMapper = progressMapper;
    }

    public List<CourseCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getEnabled, 1)
                .orderByAsc(CourseCategory::getSortNo)
                .orderByDesc(CourseCategory::getId));
    }

    public List<Course> listCourses(Long categoryId) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<Course>()
                .eq(Course::getEnabled, 1)
                .orderByAsc(Course::getSortNo)
                .orderByDesc(Course::getId);
        if (categoryId != null) {
            qw.eq(Course::getCategoryId, categoryId);
        }
        return courseMapper.selectList(qw);
    }

    public Course getCourse(Long courseId) {
        return courseMapper.selectById(courseId);
    }

    public List<CourseLesson> listLessons(Long courseId) {
        return lessonMapper.selectList(new LambdaQueryWrapper<CourseLesson>()
                .eq(CourseLesson::getEnabled, 1)
                .eq(CourseLesson::getCourseId, courseId)
                .orderByAsc(CourseLesson::getSortNo)
                .orderByAsc(CourseLesson::getId));
    }

    public CourseLesson getLesson(Long lessonId) {
        return lessonMapper.selectById(lessonId);
    }

    public StudyProgress getMyProgress(Long userId, Long courseId) {
        return progressMapper.selectOne(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getUserId, userId)
                .eq(StudyProgress::getCourseId, courseId));
    }

    public List<StudyProgress> listMyProgress(Long userId) {
        return progressMapper.selectList(new LambdaQueryWrapper<StudyProgress>()
                .eq(StudyProgress::getUserId, userId)
                .orderByDesc(StudyProgress::getLastStudyTime));
    }

    public void upsertProgress(Long userId, Long courseId, Long lessonId, Integer progressPercent, String status) {
        if (progressPercent == null) progressPercent = 0;
        if (progressPercent < 0) progressPercent = 0;
        if (progressPercent > 100) progressPercent = 100;
        if (status == null || status.trim().isEmpty()) status = "LEARNING";

        StudyProgress exist = getMyProgress(userId, courseId);
        if (exist == null) {
            StudyProgress p = new StudyProgress();
            p.setUserId(userId);
            p.setCourseId(courseId);
            p.setLessonId(lessonId);
            p.setProgressPercent(progressPercent);
            p.setStatus(status);
            p.setLastStudyTime(LocalDateTime.now());
            progressMapper.insert(p);
        } else {
            exist.setLessonId(lessonId);
            exist.setProgressPercent(progressPercent);
            exist.setStatus(status);
            exist.setLastStudyTime(LocalDateTime.now());
            progressMapper.updateById(exist);
        }
    }
}
