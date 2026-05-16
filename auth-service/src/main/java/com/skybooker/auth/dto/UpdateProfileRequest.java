package com.skybooker.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

	@NotBlank
	private String fullName;

	@NotBlank
	private String phone;

	private String passportNumber;
	private String nationality;
}
