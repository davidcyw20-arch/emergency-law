package com.yunxian.emergencylaw.test.controller;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.test.dto.TestSubmitRequest;
import com.yunxian.emergencylaw.test.entity.TestRecord;
import com.yunxian.emergencylaw.test.service.TestService;
import com.yunxian.emergencylaw.test.vo.TestPaperDetailVO;
import com.yunxian.emergencylaw.test.vo.TestPaperListVO;
import com.yunxian.emergencylaw.service.TestRulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final TestRulesService testRulesService;

    @GetMapping("/papers")
    public ApiResponse<List<TestPaperListVO>> papers() {
        return ApiResponse.ok(testService.listPapers());
    }

    @GetMapping("/paper/{paperId}")
    public ApiResponse<TestPaperDetailVO> paperDetail(@PathVariable Long paperId) {
        TestPaperDetailVO vo = testService.paperDetail(paperId);
        if (vo == null) return ApiResponse.fail("paper not found");
        return ApiResponse.ok(vo);
    }

    @GetMapping("/rules")
    public ApiResponse<TestRulesService.TestRules> rules() {
        return ApiResponse.ok(testRulesService.getRules());
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody TestSubmitRequest req, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");
        return ApiResponse.ok(testService.submit(userId, req));
    }

    @GetMapping("/my/history")
    public ApiResponse<List<TestRecord>> myHistory(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return ApiResponse.fail("Missing token");
        return ApiResponse.ok(testService.myHistory(userId));
    }

    private Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute("userId");
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
