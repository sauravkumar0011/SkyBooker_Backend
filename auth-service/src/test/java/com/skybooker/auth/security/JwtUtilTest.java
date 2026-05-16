package com.skybooker.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(
                jwtUtil,
                "secretKey",
                "my-super-secret-key-my-super-secret-key-123456"
        );

        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtExpiration",
                1000L * 60 * 60
        );
    }

    @Test
    void generateToken_extractEmailAndRole_success() {
        String token = jwtUtil.generateToken("himanshu@example.com", "PASSENGER");

        assertNotNull(token);
        assertEquals("himanshu@example.com", jwtUtil.extractEmail(token));
        assertEquals("PASSENGER", jwtUtil.extractRole(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_withUserDetails_success() {
        String token = jwtUtil.generateToken("himanshu@example.com", "ADMIN");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("himanshu@example.com")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();

        assertTrue(jwtUtil.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_whenUsernameDifferent_returnFalse() {
        String token = jwtUtil.generateToken("himanshu@example.com", "ADMIN");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("other@example.com")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();

        assertFalse(jwtUtil.isTokenValid(token, userDetails));
    }
}