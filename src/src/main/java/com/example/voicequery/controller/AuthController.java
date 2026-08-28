package com.example.voicequery.controller;

import com.example.voicequery.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    public static final String LOGIN_SESSION_KEY = "loginAdmin";

    private final AdminAuthService adminAuthService;

    public AuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect == null || redirect.isBlank() ? "/" : redirect);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(value = "redirect", required = false) String redirect,
                        HttpServletRequest request,
                        Model model) {
        if (adminAuthService.verify(username, password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute(LOGIN_SESSION_KEY, username);
            return "redirect:" + sanitizeRedirect(redirect);
        }
        model.addAttribute("error", "账号或密码错误，请重新输入。");
        model.addAttribute("redirect", redirect == null || redirect.isBlank() ? "/" : redirect);
        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String sanitizeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/";
        }
        return redirect;
    }
}
