package com.skybooker.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ResetPasswordRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(
		    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=]).*$",
		    message = "Password must contain uppercase, lowercase, number and special character"
		)
    private String newPassword;
}