package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.test.entity.TestPaper;
import com.yunxian.emergencylaw.test.entity.TestQuestion;
import com.yunxian.emergencylaw.test.mapper.TestPaperMapper;
import com.yunxian.emergencylaw.test.mapper.TestQuestionMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/test/papers")
public class AdminTestPaperController {

    private final TestPaperMapper paperMapper;
    private final TestQuestionMapper questionMapper;

    public AdminTestPaperController(TestPaperMapper paperMapper, TestQuestionMapper questionMapper) {
        this.paperMapper = paperMapper;
        this.questionMapper = questionMapper;
    }

    @GetMapping
    public ApiResponse<List<PaperVO>> list(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");

        List<TestPaper> papers = paperMapper.selectList(
                new LambdaQueryWrapper<TestPaper>().orderByDesc(TestPaper::getId)
        );
        if (papers.isEmpty()) return ApiResponse.ok(new ArrayList<>());

        List<Long> paperIds = papers.stream().map(TestPaper::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>().in(TestQuestion::getPaperId, paperIds)
        ).stream().collect(Collectors.groupingBy(TestQuestion::getPaperId, Collectors.counting()));

        List<PaperVO> out = new ArrayList<>();
        for (TestPaper p : papers) {
            PaperVO vo = new PaperVO();
            vo.setId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setDifficulty(p.getDifficulty());
            vo.setDurationMin(p.getDurationMin());
            vo.setTags(p.getTags());
            vo.setEnabled(p.getEnabled());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setQuestionCount(Math.toIntExact(countMap.getOrDefault(p.getId(), 0L)));
            out.add(vo);
        }
        return ApiResponse.ok(out);
    }

    @PostMapping
    public ApiResponse<TestPaper> create(@RequestBody PaperReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) return ApiResponse.fail("title required");

        TestPaper p = new TestPaper();
        p.setTitle(req.getTitle().trim());
        p.setDifficulty(req.getDifficulty() == null ? "EASY" : req.getDifficulty());
        p.setDurationMin(req.getDurationMin() == null ? 10 : req.getDurationMin());
        p.setTags(req.getTags());
        p.setEnabled(req.getEnabled() == null ? 0 : req.getEnabled());
        p.setCreatedAt(LocalDateTime.now());
        paperMapper.insert(p);
        return ApiResponse.ok(p);
    }

    @PutMapping("/{id}")
    public ApiResponse<TestPaper> update(@PathVariable Long id, @RequestBody PaperReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        TestPaper p = paperMapper.selectById(id);
        if (p == null) return ApiResponse.fail("not found");
        if (req.getTitle() != null) p.setTitle(req.getTitle().trim());
        if (req.getDifficulty() != null) p.setDifficulty(req.getDifficulty());
        if (req.getDurationMin() != null) p.setDurationMin(req.getDurationMin());
        if (req.getTags() != null) p.setTags(req.getTags());
        if (req.getEnabled() != null) p.setEnabled(req.getEnabled());
        paperMapper.updateById(p);
        return ApiResponse.ok(p);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<String> publish(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        TestPaper p = paperMapper.selectById(id);
        if (p == null) return ApiResponse.fail("not found");
        p.setEnabled(1);
        paperMapper.updateById(p);
        return ApiResponse.ok("published");
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<String> archive(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        TestPaper p = paperMapper.selectById(id);
        if (p == null) return ApiResponse.fail("not found");
        p.setEnabled(0);
        paperMapper.updateById(p);
        return ApiResponse.ok("archived");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    @Data
    public static class PaperReq {
        private String title;
        private String difficulty;
        private Integer durationMin;
        private String tags;
        private Integer enabled;
    }

    @Data
    public static class PaperVO {
        private Long id;
        private String title;
        private String difficulty;
        private Integer durationMin;
        private String tags;
        private Integer enabled;
        private LocalDateTime createdAt;
        private Integer questionCount;
    }
}

