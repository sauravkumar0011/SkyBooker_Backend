package com.skybooker.auth.service;

import java.util.List;
import java.util.UUID;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.UserProfileResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);
	AuthResponse login(LoginRequest request);
	AuthResponse handleOAuthLogin(String email, String name);
	
	String sendResetOtp(String email);
	String verifyResetOtp(String email, String otp);
	public String resetPasswordWithOtp(String email, String newPassword);
	
	UserProfileResponse getMyProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
    String deactivateAccount(UUID userId);
    List<UserProfileResponse> getAllUsers();
    UserProfileResponse getUserById(UUID userId);
    void deleteUser(UUID userId);
   
}
