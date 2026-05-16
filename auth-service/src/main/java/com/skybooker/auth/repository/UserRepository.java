package com.skybooker.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skybooker.auth.entity.Role;
import com.skybooker.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);
	
	Optional<User> findByProviderId(String providerId);

	Optional<User> findByPhone(String phone);

	Optional<User> findByPassportNumber(String passportNumber);

	boolean existsByEmail(String email);

	boolean existsByPhone(String phone);
	
	boolean existsByPassportNumber(String passportNumber);

	List<User> findAllByRole(Role role);


}
