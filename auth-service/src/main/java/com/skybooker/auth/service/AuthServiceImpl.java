package com.skybooker.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.PasswordResetOtpEvent;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.UserProfileResponse;
import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.exception.InvalidCredentialsException;
import com.skybooker.auth.exception.ResourceNotFoundException;
import com.skybooker.auth.exception.UserAlreadyExistsException;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;

import lombok.Data;

@Service
@Data

public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final PasswordResetOtpPublisher passwordResetOtpPublisher;

	@Override
	public AuthResponse register(RegisterRequest request) {

	    if (userRepository.existsByEmail(request.getEmail())) {
	        throw new UserAlreadyExistsException("Email already registered");
	    }

	    if (userRepository.existsByPhone(request.getPhone())) {
	        throw new UserAlreadyExistsException("Phone already registered");
	    }

	    String passportNumber = request.getPassportNumber();

	   if (passportNumber != null && passportNumber.isBlank()) {
	     passportNumber = null;
	   }

	   if (passportNumber != null && userRepository.existsByPassportNumber(passportNumber)) {
	     throw new UserAlreadyExistsException("Passport number already registered");
	   }

	    User user = User.builder()
	            .fullName(request.getFullName())
	            .email(request.getEmail())
	            .password(passwordEncoder.encode(request.getPassword()))
	            .phone(request.getPhone())
	            .role(request.getRole())
	            .provider(AuthProvider.LOCAL)
	            .passportNumber(passportNumber)
	            .nationality(request.getNationality())
	            .isActive(true)
	            .airlineId(request.getAirlineId())
	            .build();

	    User savedUser = userRepository.save(user);

	    String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

	    return buildAuthResponse(savedUser, token, "User registered successfully");
	}
	
	@Override
	public AuthResponse login(LoginRequest request) {
		
		User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return buildAuthResponse(user, token, "Login successful");
	}
	
	@Override
	public AuthResponse handleOAuthLogin(String email, String name) {

	    Optional<User> existingUser = userRepository.findByEmail(email);
	    		
	    User user;

	    if (existingUser.isPresent()) {
	        user = existingUser.get();
	    } else {
	        user = User.builder()
	                .email(email)
	                .fullName(name)
	                .provider(AuthProvider.GOOGLE)
	                .isActive(true)
	                .role(Role.PASSENGER)
	                .build();

	        userRepository.save(user);
	        user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
	    }
        
	    String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return buildAuthResponse(user, token, "Login successful");
	}
	
	@Override
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new InvalidCredentialsException("Invalid email"));
        
        return mapToProfileResponse(user);
    }
	
	 @Override
	 public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
	        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	        validateUniqueProfileFields(user, request);

	        String passportNumber = normalizeBlank(request.getPassportNumber());

	        user.setFullName(request.getFullName());
	        user.setPhone(request.getPhone());
	        user.setPassportNumber(passportNumber);
	        user.setNationality(request.getNationality());
            
	        
	        return mapToProfileResponse(userRepository.save(user));
	    }
	 
	 @Override
	 public String deactivateAccount(UUID userId) {
		    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	        user.setIsActive(false);
	        userRepository.save(user);
	        return "Account deactivated successfully";
	    }
	 
	 @Override
	 public List<UserProfileResponse> getAllUsers() {
	        return userRepository.findAll()
	                .stream()
	                .map(this::mapToProfileResponse)
	                .toList();
	    }
	 @Override
	 public UserProfileResponse getUserById(UUID userId) {
	        User user = userRepository.findById(userId)
	                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
	        return mapToProfileResponse(user);
	    }
	 
	 private UserProfileResponse mapToProfileResponse(User user) {
	        return UserProfileResponse.builder()
	        		.userId(user.getUserId())
	                .fullName(user.getFullName())
	                .email(user.getEmail())
	                .phone(user.getPhone())
	                .role(user.getRole())
	                .provider(user.getProvider())
	                .isActive(user.getIsActive())
	                .profileComplete(isProfileComplete(user))
	                .passportNumber(user.getPassportNumber())
	                .nationality(user.getNationality())
	                .build();
	    }
	 
	 @Override
	 public void deleteUser(UUID userId) {
		 userRepository.deleteById(userId);
	 }

	 private AuthResponse buildAuthResponse(User user, String token, String message) {
	 	return AuthResponse.builder()
	 			.userId(user.getUserId())
	 			.token(token)
	 			.email(user.getEmail())
	 			.fullName(user.getFullName())
	 			.role(user.getRole())
	 			.provider(user.getProvider())
	 			.profileComplete(isProfileComplete(user))
	 			.airlineId(user.getAirlineId())
	 			.message(message)
	 			.build();
	 }

	 @Override
	 public String sendResetOtp(String email) {

	     User user = userRepository.findByEmail(email)
	             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	     String otp = String.valueOf(new SecureRandom().nextInt(900000) + 100000);

	     user.setResetOtp(passwordEncoder.encode(otp));
	     user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
	     user.setResetOtpVerified(false);

	     userRepository.save(user);
	    
	     passwordResetOtpPublisher.publish(
	    	        PasswordResetOtpEvent.builder()
	    	                .userId(user.getUserId())
	    	                .email(user.getEmail())
	    	                .fullName(user.getFullName())
	    	                .otp(otp) 
	    	                .expiresAt(user.getResetOtpExpiry())
	    	                .build()
	    	);

	     return "OTP sent to your email";
	 }
	 
	 
	 @Override
	 public String verifyResetOtp(String email, String otp) {

	     User user = userRepository.findByEmail(email)
	             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	     if (user.getResetOtp() == null || !passwordEncoder.matches(otp, user.getResetOtp())) {
	         throw new RuntimeException("Invalid OTP");
	     }

	     if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
	         throw new RuntimeException("OTP expired");
	     }

	     user.setResetOtpVerified(true);
	     userRepository.save(user);

	     return "OTP verified successfully";
	 }
	 
	 @Override
	 public String resetPasswordWithOtp(String email, String newPassword) {

	     User user = userRepository.findByEmail(email)
	             .orElseThrow(() -> new ResourceNotFoundException("User not found"));

	     if (!Boolean.TRUE.equals(user.getResetOtpVerified())) {
	         throw new RuntimeException("OTP not verified");
	     }

	     user.setPassword(passwordEncoder.encode(newPassword));

	     user.setResetOtp(null);
	     user.setResetOtpExpiry(null);
	     user.setResetOtpVerified(false);

	     userRepository.save(user);

	     return "Password reset successfully";
	 }
	 
	 private void validateUniqueProfileFields(User currentUser, UpdateProfileRequest request) {
	 	userRepository.findByPhone(request.getPhone())
	 			.filter(existingUser -> !existingUser.getUserId().equals(currentUser.getUserId()))
	 			.ifPresent(existingUser -> {
	 				throw new UserAlreadyExistsException("Phone already registered");
	 			});

	 	String passportNumber = normalizeBlank(request.getPassportNumber());
	 	if (!StringUtils.hasText(passportNumber)) {
	 		return;
	 	}

	 	userRepository.findByPassportNumber(passportNumber)
	 			.filter(existingUser -> !existingUser.getUserId().equals(currentUser.getUserId()))
	 			.ifPresent(existingUser -> {
	 				throw new UserAlreadyExistsException("Passport number already registered");
	 			});
	 }

	 private String normalizeBlank(String value) {
	 	return StringUtils.hasText(value) ? value.trim() : null;
	 }

	 private boolean isProfileComplete(User user) {
	 	return StringUtils.hasText(user.getPhone());
	 }
}
