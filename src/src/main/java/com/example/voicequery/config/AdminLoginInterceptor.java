package com.example.voicequery.config;

import com.example.voicequery.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AdminLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object loginAdmin = request.getSession().getAttribute(AuthController.LOGIN_SESSION_KEY);
        if (loginAdmin != null) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "请先登录后再访问查询接口。");
            return false;
        }

        String redirect = uri;
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            redirect += "?" + query;
        }
        response.sendRedirect("/login?redirect=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
        return false;
    }
}
