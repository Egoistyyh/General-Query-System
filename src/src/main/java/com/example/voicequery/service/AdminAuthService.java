package com.example.voicequery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final String username;
    private final String password;

    public AdminAuthService(@Value("${app.admin.username:admin}") String username,
                            @Value("${app.admin.password:admin123}") String password) {
        this.username = username;
        this.password = password;
    }

    public boolean verify(String inputUsername, String inputPassword) {
        return username.equals(inputUsername) && password.equals(inputPassword);
    }
}
