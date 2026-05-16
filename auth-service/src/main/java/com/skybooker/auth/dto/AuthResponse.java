package com.skybooker.auth.dto;

import java.util.UUID;

import com.skybooker.auth.entity.AuthProvider;
import com.skybooker.auth.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
	
	private UUID userId;
    private String token;
    private String email;
    private String fullName;
    private Role role;
    private AuthProvider provider;
    private Boolean profileComplete;
    private String message;
    private UUID airlineId;
}
