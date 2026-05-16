package com.skybooker.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ForgotPasswordRequest {
	
    @Email
    @NotBlank
    private String email;
}