package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.entity.Course;
import com.yunxian.emergencylaw.entity.SharePost;
import com.yunxian.emergencylaw.entity.StudyProgress;
import com.yunxian.emergencylaw.entity.SysUser;
import com.yunxian.emergencylaw.mapper.CourseMapper;
import com.yunxian.emergencylaw.mapper.SharePostMapper;
import com.yunxian.emergencylaw.mapper.StudyProgressMapper;
import com.yunxian.emergencylaw.mapper.SysUserMapper;
import com.yunxian.emergencylaw.service.ShareModerationService;
import com.yunxian.emergencylaw.service.UserDisableService;
import com.yunxian.emergencylaw.test.entity.TestPaper;
import com.yunxian.emergencylaw.test.entity.TestQuestion;
import com.yunxian.emergencylaw.test.entity.TestRecord;
import com.yunxian.emergencylaw.test.mapper.TestPaperMapper;
import com.yunxian.emergencylaw.test.mapper.TestQuestionMapper;
import com.yunxian.emergencylaw.test.mapper.TestRecordMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;
    private final SharePostMapper sharePostMapper;
    private final StudyProgressMapper studyProgressMapper;
    private final TestPaperMapper testPaperMapper;
    private final TestQuestionMapper testQuestionMapper;
    private final TestRecordMapper testRecordMapper;
    private final ShareModerationService shareModerationService;
    private final UserDisableService userDisableService;

    public AdminReportController(SysUserMapper sysUserMapper,
                                 CourseMapper courseMapper,
                                 SharePostMapper sharePostMapper,
                                 StudyProgressMapper studyProgressMapper,
                                 TestPaperMapper testPaperMapper,
                                 TestQuestionMapper testQuestionMapper,
                                 TestRecordMapper testRecordMapper,
                                 ShareModerationService shareModerationService,
                                 UserDisableService userDisableService) {
        this.sysUserMapper = sysUserMapper;
        this.courseMapper = courseMapper;
        this.sharePostMapper = sharePostMapper;
        this.studyProgressMapper = studyProgressMapper;
        this.testPaperMapper = testPaperMapper;
        this.testQuestionMapper = testQuestionMapper;
        this.testRecordMapper = testRecordMapper;
        this.shareModerationService = shareModerationService;
        this.userDisableService = userDisableService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewVO> overview(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");

        long userCount = sysUserMapper.selectCount(null);
        long disabledUsers = userDisableService.snapshot().size();

        long courseCount = courseMapper.selectCount(null);
        long coursePublished = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getEnabled, 1));

        long postCount = sharePostMapper.selectCount(null);
        List<SharePost> posts = sharePostMapper.selectList(new LambdaQueryWrapper<SharePost>().select(SharePost::getId));
        long hiddenPostCount = posts.stream().filter(p -> shareModerationService.isHidden(p.getId())).count();
        long visiblePostCount = Math.max(0, postCount - hiddenPostCount);

        long progressCount = studyProgressMapper.selectCount(null);

        long paperCount = testPaperMapper.selectCount(null);
        long paperPublished = testPaperMapper.selectCount(new LambdaQueryWrapper<TestPaper>().eq(TestPaper::getEnabled, 1));
        long questionCount = testQuestionMapper.selectCount(null);
        long recordCount = testRecordMapper.selectCount(null);

        OverviewVO vo = new OverviewVO();
        vo.setUserCount(userCount);
        vo.setDisabledUserCount(disabledUsers);
        vo.setCourseCount(courseCount);
        vo.setPublishedCourseCount(coursePublished);
        vo.setPostCount(postCount);
        vo.setVisiblePostCount(visiblePostCount);
        vo.setHiddenPostCount(hiddenPostCount);
        vo.setPaperCount(paperCount);
        vo.setPublishedPaperCount(paperPublished);
        vo.setQuestionCount(questionCount);
        vo.setTestRecordCount(recordCount);
        vo.setStudyProgressCount(progressCount);
        return ApiResponse.ok(vo);
    }

    @GetMapping("/monthly")
    public ApiResponse<PeriodReportVO> monthly(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        PeriodReportVO vo = buildPeriodReport("月报", start, end);
        return ApiResponse.ok(vo);
    }

    @GetMapping("/quarterly")
    public ApiResponse<PeriodReportVO> quarterly(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int q = (month - 1) / 3; // 0..3
        LocalDate start = LocalDate.of(now.getYear(), q * 3 + 1, 1);
        LocalDate end = start.plusMonths(3);
        PeriodReportVO vo = buildPeriodReport("季报", start, end);
        return ApiResponse.ok(vo);
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    private PeriodReportVO buildPeriodReport(String title, LocalDate startDate, LocalDate endDateExclusive) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDateExclusive.atStartOfDay();

        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getCreateTime)
                        .ge(SysUser::getCreateTime, start)
                        .lt(SysUser::getCreateTime, end)
        );

        List<SharePost> posts = sharePostMapper.selectList(
                new LambdaQueryWrapper<SharePost>()
                        .select(SharePost::getId, SharePost::getCreatedAt)
                        .ge(SharePost::getCreatedAt, start)
                        .lt(SharePost::getCreatedAt, end)
        );

        List<TestRecord> records = testRecordMapper.selectList(
                new LambdaQueryWrapper<TestRecord>()
                        .select(TestRecord::getId, TestRecord::getCreatedAt)
                        .ge(TestRecord::getCreatedAt, start)
                        .lt(TestRecord::getCreatedAt, end)
        );

        List<StudyProgress> progressList = studyProgressMapper.selectList(
                new LambdaQueryWrapper<StudyProgress>()
                        .select(StudyProgress::getUserId, StudyProgress::getLastStudyTime)
                        .ge(StudyProgress::getLastStudyTime, start)
                        .lt(StudyProgress::getLastStudyTime, end)
        );

        Map<LocalDate, Integer> userByDay = countByDay(users, u -> u.getCreateTime());

        Map<LocalDate, Integer> postByDay = new HashMap<>();
        Map<LocalDate, Integer> hiddenPostByDay = new HashMap<>();
        for (SharePost p : posts) {
            if (p.getCreatedAt() == null) continue;
            LocalDate d = p.getCreatedAt().toLocalDate();
            postByDay.put(d, postByDay.getOrDefault(d, 0) + 1);
            if (shareModerationService.isHidden(p.getId())) {
                hiddenPostByDay.put(d, hiddenPostByDay.getOrDefault(d, 0) + 1);
            }
        }

        Map<LocalDate, Integer> recordByDay = countByDay(records, r -> r.getCreatedAt());

        Map<LocalDate, Set<Long>> activeUsersByDay = new HashMap<>();
        Set<Long> activeUsersAll = new HashSet<>();
        for (StudyProgress p : progressList) {
            if (p.getLastStudyTime() == null || p.getUserId() == null) continue;
            LocalDate d = p.getLastStudyTime().toLocalDate();
            activeUsersByDay.computeIfAbsent(d, k -> new HashSet<>()).add(p.getUserId());
            activeUsersAll.add(p.getUserId());
        }

        PeriodReportVO vo = new PeriodReportVO();
        vo.setTitle(title);
        vo.setRangeStart(startDate.toString());
        vo.setRangeEndExclusive(endDateExclusive.toString());
        vo.setNewUsers(users.size());
        vo.setNewPosts(posts.size());
        vo.setHiddenPosts(posts.stream().filter(p -> shareModerationService.isHidden(p.getId())).count());
        vo.setTestSubmissions(records.size());
        vo.setActiveLearners(activeUsersAll.size());

        long days = ChronoUnit.DAYS.between(startDate, endDateExclusive);
        List<PeriodReportVO.DayVO> daily = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            PeriodReportVO.DayVO dv = new PeriodReportVO.DayVO();
            dv.setDate(d.toString());
            dv.setNewUsers(userByDay.getOrDefault(d, 0));
            dv.setNewPosts(postByDay.getOrDefault(d, 0));
            dv.setHiddenPosts(hiddenPostByDay.getOrDefault(d, 0));
            dv.setTestSubmissions(recordByDay.getOrDefault(d, 0));
            dv.setActiveLearners(activeUsersByDay.getOrDefault(d, new HashSet<>()).size());
            daily.add(dv);
        }
        vo.setDaily(daily);
        return vo;
    }

    private interface DateGetter<T> {
        LocalDateTime get(T t);
    }

    private <T> Map<LocalDate, Integer> countByDay(List<T> list, DateGetter<T> getter) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (T t : list) {
            LocalDateTime dt = getter.get(t);
            if (dt == null) continue;
            LocalDate d = dt.toLocalDate();
            map.put(d, map.getOrDefault(d, 0) + 1);
        }
        return map;
    }

    @Data
    public static class OverviewVO {
        private long userCount;
        private long disabledUserCount;
        private long courseCount;
        private long publishedCourseCount;
        private long postCount;
        private long visiblePostCount;
        private long hiddenPostCount;
        private long paperCount;
        private long publishedPaperCount;
        private long questionCount;
        private long testRecordCount;
        private long studyProgressCount;
    }

    @Data
    public static class PeriodReportVO {
        private String title;
        private String rangeStart;
        private String rangeEndExclusive;

        private long newUsers;
        private long newPosts;
        private long hiddenPosts;
        private long testSubmissions;
        private long activeLearners;

        private List<DayVO> daily;

        @Data
        public static class DayVO {
            private String date;
            private int newUsers;
            private int newPosts;
            private int hiddenPosts;
            private int testSubmissions;
            private int activeLearners;
        }
    }
}
