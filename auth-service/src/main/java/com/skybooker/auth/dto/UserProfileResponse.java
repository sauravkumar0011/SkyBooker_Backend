package com.skybooker.auth.dto;

import java.util.UUID;

import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

	private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AuthProvider provider;
    private Boolean isActive;
    private Boolean profileComplete;
    private String passportNumber;
    private String nationality;
}
