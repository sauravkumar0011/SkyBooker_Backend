package com.skybooker.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID userId;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column
	private String password;
	
	@Column(unique = true)
	private String phone;

	@Column(unique = true)
	private String providerId;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(unique = true)
    private String passportNumber;

    private String nationality;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String resetOtp;

    private LocalDateTime resetOtpExpiry;

    private Boolean resetOtpVerified = false;
    
    private UUID airlineId;
    
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.provider == null) this.provider = AuthProvider.LOCAL;
    }
}
