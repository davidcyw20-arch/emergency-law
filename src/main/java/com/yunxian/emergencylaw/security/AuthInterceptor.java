package com.yunxian.emergencylaw.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 预检请求直接放行（跨域 OPTIONS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 登录/注册 放行
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return true;
        }

        // 读取 Authorization
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.trim().isEmpty()) {
            writeJson(response, 200, "{\"success\":false,\"message\":\"Missing token\",\"data\":null}");
            return false;
        }

        // 兼容两种格式：
        // 1) Authorization: Bearer <token>
        // 2) Authorization: <token>
        String token = auth.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        if (token.isEmpty()) {
            writeJson(response, 200, "{\"success\":false,\"message\":\"Missing token\",\"data\":null}");
            return false;
        }

        try {
            Claims claims = jwtUtil.parseClaims(token);

            String sub = claims.getSubject(); // userId
            if (sub == null || sub.trim().isEmpty()) {
                writeJson(response, 200, "{\"success\":false,\"message\":\"Invalid token\",\"data\":null}");
                return false;
            }

            // 写入 request，Controller 可通过 request.getAttribute(...) 获取
            request.setAttribute("userId", Long.parseLong(sub));
            request.setAttribute("username", String.valueOf(claims.get("username")));
            request.setAttribute("role", String.valueOf(claims.get("role")));

            return true;

        } catch (Exception e) {
            writeJson(response, 200, "{\"success\":false,\"message\":\"Invalid token\",\"data\":null}");
            return false;
        }
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
