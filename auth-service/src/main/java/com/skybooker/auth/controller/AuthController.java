package com.skybooker.auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.ForgotPasswordRequest;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.ResetPasswordRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.UserProfileResponse;
import com.skybooker.auth.dto.VerifyOtpRequest;
import com.skybooker.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
		return new ResponseEntity<>(authService.register(request),HttpStatus.CREATED);
	}
	
	@PostMapping("/register/staff")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<AuthResponse> registerStaff(@Valid @RequestBody RegisterRequest request){
		return new ResponseEntity<>(authService.register(request),HttpStatus.CREATED);
	}
	
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
		return ResponseEntity.ok(authService.login(request));
	}
	
	@GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(authService.getMyProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(Authentication authentication,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }
    
    @PutMapping("users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivateAccount(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.deactivateAccount(userId));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }
    
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUserById(@PathVariable UUID userId){
    	authService.deleteUser(userId);
    	return ResponseEntity.ok("User deleted successfully");
    }
    
    @PostMapping("/forgot-password/otp")
    public ResponseEntity<String> sendResetOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.sendResetOtp(request.getEmail()));
    }
    
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<String> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(
                authService.verifyResetOtp(request.getEmail(), request.getOtp())
        );
    }
    
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<String> resetPasswordWithOtp(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(
                authService.resetPasswordWithOtp(request.getEmail(), request.getNewPassword())
        );
    }
    
}
