package com.skybooker.auth.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        // Reuse your existing service logic
        AuthResponse res  = authService.handleOAuthLogin(email, name);

        // Redirect to frontend with token
        String redirectUrl = "http://localhost:4200/oauth-success" +
                "?token=" + encode(res.getToken()) +
                "&userId=" + encode(String.valueOf(res.getUserId())) +
                "&email=" + encode(res.getEmail()) +
                "&name=" + encode(res.getFullName()) +
                "&role=" + encode(String.valueOf(res.getRole())) +
                "&message=" + encode(res.getMessage());

        response.sendRedirect(redirectUrl);
    }
    
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}