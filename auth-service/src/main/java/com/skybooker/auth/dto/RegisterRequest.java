package com.skybooker.auth.dto;

import jakarta.validation.constraints.Pattern;

import java.util.UUID;

import com.skybooker.auth.entity.Role;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

	@NotBlank
	private String fullName;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(
		    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=]).*$",
		    message = "Password must contain uppercase, lowercase, number and special character"
		)
	private String password;
	
	@NotBlank
    private String phone;
	
	@NotNull
    private Role role;
	
	@Pattern(regexp = "^[A-Z0-9]{6,9}$", message = "Invalid passport number")
	private String passportNumber;
	
    private String nationality;
    
    private UUID airlineId;
}
