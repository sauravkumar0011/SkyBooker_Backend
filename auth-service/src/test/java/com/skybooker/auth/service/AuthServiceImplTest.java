package com.skybooker.auth.service;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .fullName("Himanshu Kumar")
                .email("himanshu@example.com")
                .password("encodedPassword")
                .phone("9876543210")
                .role(Role.PASSENGER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .passportNumber("A123456")
                .nationality("Indian")
                .build();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Himanshu Kumar");
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");
        request.setPhone("9876543210");
        request.setRole(Role.PASSENGER);
        request.setPassportNumber("A123456");
        request.setNationality("Indian");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(userRepository.existsByPassportNumber(request.getPassportNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("himanshu@example.com", response.getEmail());
        assertEquals("Himanshu Kumar", response.getFullName());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals("User registered successfully", response.getMessage());

        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken("himanshu@example.com", "PASSENGER");
    }

    @Test
    void register_whenEmailAlreadyExists_throwException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("himanshu@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPhoneAlreadyExists_throwException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("himanshu@example.com");
        request.setPhone("9876543210");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertEquals("Phone already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPassportAlreadyExists_throwException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("himanshu@example.com");
        request.setPhone("9876543210");
        request.setPassportNumber("A123456");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(userRepository.existsByPassportNumber(request.getPassportNumber())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );

        assertEquals("Passport number already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenPassportBlank_shouldSavePassportAsNull() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Himanshu Kumar");
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");
        request.setPhone("9876543210");
        request.setRole(Role.PASSENGER);
        request.setPassportNumber(" ");
        request.setNationality("Indian");

        User savedUser = User.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password("encodedPassword")
                .phone(request.getPhone())
                .role(Role.PASSENGER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .passportNumber(null)
                .nationality("Indian")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.getToken());
        verify(userRepository, never()).existsByPassportNumber(anyString());
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("himanshu@example.com", response.getEmail());
        assertEquals("Login successful", response.getMessage());

        verify(jwtUtil).generateToken("himanshu@example.com", "PASSENGER");
    }

    @Test
    void login_whenEmailNotFound_throwInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("Password@123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_whenAccountDeactivated_throwInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("himanshu@example.com");
        request.setPassword("Password@123");

        user.setIsActive(false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Account is deactivated", exception.getMessage());
    }

    @Test
    void login_whenPasswordIncorrect_throwInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("himanshu@example.com");
        request.setPassword("WrongPassword");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void getMyProfile_success() {
        when(userRepository.findByEmail("himanshu@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getMyProfile("himanshu@example.com");

        assertEquals(userId, response.getUserId());
        assertEquals("Himanshu Kumar", response.getFullName());
        assertEquals("himanshu@example.com", response.getEmail());
        assertEquals("9876543210", response.getPhone());
        assertEquals(Role.PASSENGER, response.getRole());
        assertTrue(response.getIsActive());
    }

    @Test
    void getMyProfile_whenUserNotFound_throwInvalidCredentialsException() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.getMyProfile("wrong@example.com")
        );
    }

    @Test
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setPhone("9999999999");
        request.setPassportNumber("B987654");
        request.setNationality("Indian");

        User updatedUser = User.builder()
                .userId(userId)
                .fullName("Updated Name")
                .email("himanshu@example.com")
                .password("encodedPassword")
                .phone("9999999999")
                .role(Role.PASSENGER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .passportNumber("B987654")
                .nationality("Indian")
                .build();

        when(userRepository.findByEmail("himanshu@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserProfileResponse response = authService.updateProfile("himanshu@example.com", request);

        assertEquals("Updated Name", response.getFullName());
        assertEquals("9999999999", response.getPhone());
        assertEquals("B987654", response.getPassportNumber());
    }

    @Test
    void updateProfile_whenUserNotFound_throwResourceNotFoundException() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.updateProfile("wrong@example.com", request)
        );
    }

    @Test
    void deactivateAccount_success() {

        UUID userId = UUID.randomUUID();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String response = authService.deactivateAccount(userId);

        assertEquals("Account deactivated successfully", response);
        assertFalse(user.getIsActive());

        verify(userRepository).save(user);
    }

    @Test
    void deactivateAccount_whenUserNotFound_throwResourceNotFoundException() {

        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.deactivateAccount(userId)
        );
    }

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserProfileResponse> response = authService.getAllUsers();

        assertEquals(1, response.size());
        assertEquals("himanshu@example.com", response.get(0).getEmail());
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getUserById(userId);

        assertEquals(userId, response.getUserId());
        assertEquals("Himanshu Kumar", response.getFullName());
    }

    @Test
    void getUserById_whenUserNotFound_throwResourceNotFoundException() {
        UUID wrongId = UUID.randomUUID();

        when(userRepository.findById(wrongId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getUserById(wrongId)
        );
    }
}
