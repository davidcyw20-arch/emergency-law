package com.yunxian.emergencylaw.controller.admin;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.service.TestRulesService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/test/rules")
public class AdminTestRulesController {

    private final TestRulesService testRulesService;

    public AdminTestRulesController(TestRulesService testRulesService) {
        this.testRulesService = testRulesService;
    }

    @GetMapping
    public ApiResponse<TestRulesService.TestRules> get(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        return ApiResponse.ok(testRulesService.getRules());
    }

    @PutMapping
    public ApiResponse<TestRulesService.TestRules> save(@RequestBody TestRulesService.TestRules rules, HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        return ApiResponse.ok(testRulesService.saveRules(rules));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }
}

