package com.yunxian.emergencylaw.controller.admin;

import com.yunxian.emergencylaw.common.ApiResponse;
import com.yunxian.emergencylaw.test.service.TestBankSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/test")
public class AdminTestSeedController {

    private final TestBankSeedService testBankSeedService;

    public AdminTestSeedController(TestBankSeedService testBankSeedService) {
        this.testBankSeedService = testBankSeedService;
    }

    @PostMapping("/seed")
    public ApiResponse<TestBankSeedService.SeedResult> seed(HttpServletRequest request) {
        if (!isAdmin(request)) return ApiResponse.fail("no permission");
        return ApiResponse.ok(testBankSeedService.seed(false));
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null && "ADMIN".equalsIgnoreCase(String.valueOf(role));
    }
}

