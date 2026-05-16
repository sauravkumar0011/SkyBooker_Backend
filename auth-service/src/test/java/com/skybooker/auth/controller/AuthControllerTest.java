package com.skybooker.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.UserProfileResponse;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Himanshu Kumar");
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");
        request.setPhone("9876543210");
        request.setRole(Role.PASSENGER);
        request.setPassportNumber("A123456");
        request.setNationality("Indian");

        AuthResponse response = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .role(Role.PASSENGER)
                .token("jwt-token")
                .message("User registered successfully")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("himanshu@example.com"))
                .andExpect(jsonPath("$.fullName").value("Himanshu Kumar"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");

        AuthResponse response = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .role(Role.PASSENGER)
                .token("jwt-token")
                .message("Login successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("himanshu@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void getProfile_success() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("himanshu@example.com", null);

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(UUID.randomUUID())
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .phone("9876543210")
                .role(Role.PASSENGER)
                .isActive(true)
                .passportNumber("A123456")
                .nationality("Indian")
                .build();

        when(authService.getMyProfile("himanshu@example.com")).thenReturn(response);

        mockMvc.perform(get("/auth/profile")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("himanshu@example.com"))
                .andExpect(jsonPath("$.fullName").value("Himanshu Kumar"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    void updateProfile_success() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("himanshu@example.com", null);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setPhone("9999999999");
        request.setPassportNumber("B987654");
        request.setNationality("Indian");

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(UUID.randomUUID())
                .fullName("Updated Name")
                .email("himanshu@example.com")
                .phone("9999999999")
                .role(Role.PASSENGER)
                .isActive(true)
                .passportNumber("B987654")
                .nationality("Indian")
                .build();

        when(authService.updateProfile(eq("himanshu@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/auth/profile")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("9999999999"))
                .andExpect(jsonPath("$.passportNumber").value("B987654"));
    }

    @Test
    void deactivateAccount_success() throws Exception {

        UUID userId = UUID.randomUUID();

        when(authService.deactivateAccount(userId))
                .thenReturn("Account deactivated successfully");

        mockMvc.perform(put("/auth/users/{userId}/deactivate", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deactivated successfully"));
    }

    @Test
    void getAllUsers_success() throws Exception {
        UserProfileResponse userResponse = UserProfileResponse.builder()
                .userId(UUID.randomUUID())
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .phone("9876543210")
                .role(Role.PASSENGER)
                .isActive(true)
                .passportNumber("A123456")
                .nationality("Indian")
                .build();

        when(authService.getAllUsers()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("himanshu@example.com"))
                .andExpect(jsonPath("$[0].fullName").value("Himanshu Kumar"));
    }

    @Test
    void getUserById_success() throws Exception {
        UUID userId = UUID.randomUUID();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(userId)
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .phone("9876543210")
                .role(Role.PASSENGER)
                .isActive(true)
                .passportNumber("A123456")
                .nationality("Indian")
                .build();

        when(authService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/auth/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("himanshu@example.com"));
    }
}