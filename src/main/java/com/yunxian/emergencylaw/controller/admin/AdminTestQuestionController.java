package com.yunxian.emergencylaw.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.test.entity.TestQuestion;
import com.yunxian.emergencylaw.test.mapper.TestQuestionMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/test/papers/{paperId}/questions")
public class AdminTestQuestionController {

    private final TestQuestionMapper questionMapper;

    public AdminTestQuestionController(TestQuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @GetMapping
    public ApiResponse<List<TestQuestion>> list(@PathVariable Long paperId, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        List<TestQuestion> list = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>()
                        .eq(TestQuestion::getPaperId, paperId)
                        .orderByAsc(TestQuestion::getSortNo)
                        .orderByAsc(TestQuestion::getId)
        );
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<TestQuestion> create(@PathVariable Long paperId, @RequestBody QuestionReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        if (req.getStem() == null || req.getStem().trim().isEmpty()) return ApiResponse.fail("stem required");
        if (req.getOptionA() == null || req.getOptionB() == null || req.getOptionC() == null || req.getOptionD() == null) {
            return ApiResponse.fail("options required");
        }
        if (req.getAnswerIndex() == null || req.getAnswerIndex() < 0 || req.getAnswerIndex() > 3) {
            return ApiResponse.fail("answerIndex invalid");
        }

        TestQuestion q = new TestQuestion();
        q.setPaperId(paperId);
        q.setStem(req.getStem().trim());
        q.setOptionA(req.getOptionA());
        q.setOptionB(req.getOptionB());
        q.setOptionC(req.getOptionC());
        q.setOptionD(req.getOptionD());
        q.setAnswerIndex(req.getAnswerIndex());
        q.setSortNo(req.getSortNo() == null ? 1 : req.getSortNo());
        q.setCreatedAt(LocalDateTime.now());
        questionMapper.insert(q);
        return ApiResponse.ok(q);
    }

    @PutMapping("/{id}")
    public ApiResponse<TestQuestion> update(@PathVariable Long paperId, @PathVariable Long id, @RequestBody QuestionReq req, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        TestQuestion q = questionMapper.selectById(id);
        if (q == null || q.getPaperId() == null || !q.getPaperId().equals(paperId)) return ApiResponse.fail("not found");

        if (req.getStem() != null) q.setStem(req.getStem().trim());
        if (req.getOptionA() != null) q.setOptionA(req.getOptionA());
        if (req.getOptionB() != null) q.setOptionB(req.getOptionB());
        if (req.getOptionC() != null) q.setOptionC(req.getOptionC());
        if (req.getOptionD() != null) q.setOptionD(req.getOptionD());
        if (req.getAnswerIndex() != null) q.setAnswerIndex(req.getAnswerIndex());
        if (req.getSortNo() != null) q.setSortNo(req.getSortNo());
        questionMapper.updateById(q);
        return ApiResponse.ok(q);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long paperId, @PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        TestQuestion q = questionMapper.selectById(id);
        if (q == null || q.getPaperId() == null || !q.getPaperId().equals(paperId)) return ApiResponse.fail("not found");
        questionMapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }

    @Data
    public static class QuestionReq {
        private String stem;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private Integer answerIndex;
        private Integer sortNo;
    }
}

