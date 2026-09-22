package com.rahul.userservice.user;

import com.rahul.userservice.auth.JwtUtil;
import com.rahul.userservice.common.exception.InvalidCredentialsException;
import com.rahul.userservice.common.exception.ResourceNotFoundException;
import com.rahul.userservice.common.exception.UserAlreadyExistsException;
import com.rahul.userservice.user.dto.AuthResponse;
import com.rahul.userservice.user.dto.ChangePasswordRequest;
import com.rahul.userservice.user.dto.LoginRequest;
import com.rahul.userservice.user.dto.RegisterRequest;
import com.rahul.userservice.user.dto.UpdateProfileRequest;
import com.rahul.userservice.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;



    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = RegisterRequest.builder()
                .email("rahul@gmail.com")
                .password("password123")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        UUID userId = UUID.randomUUID();

        User savedUser = User.builder()
                .id(userId)
                .email("rahul@gmail.com")
                .password("encodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail("rahul@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponse response =
                userService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail())
                .isEqualTo("rahul@gmail.com");
        assertThat(response.getFirstName())
                .isEqualTo("Rahul");
        assertThat(response.getLastName())
                .isEqualTo("Kushwaha");
        assertThat(response.getRole())
                .isEqualTo(Role.CUSTOMER);

        verify(userRepository)
                .existsByEmail("rahul@gmail.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }


    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterRequest request = RegisterRequest.builder()
                .email("rahul@gmail.com")
                .password("password123")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.existsByEmail("rahul@gmail.com"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(request)
        )
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already registered");

        verify(userRepository)
                .existsByEmail("rahul@gmail.com");

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }




    @Test
    void shouldLoginSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("rahul@gmail.com")
                .password("encodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("rahul@gmail.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        ))
                .thenReturn(true);

        when(jwtUtil.generateToken(
                "rahul@gmail.com",
                Role.CUSTOMER.name()
        ))
                .thenReturn("jwt-token");

        AuthResponse response =
                userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getId())
                .isEqualTo(userId);
        assertThat(response.getEmail())
                .isEqualTo("rahul@gmail.com");
        assertThat(response.getRole())
                .isEqualTo(Role.CUSTOMER);
        assertThat(response.getToken())
                .isEqualTo("jwt-token");

        verify(userRepository)
                .findByEmail("rahul@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encodedPassword"
                );

        verify(jwtUtil)
                .generateToken(
                        "rahul@gmail.com",
                        Role.CUSTOMER.name()
                );
    }


    @Test
    void shouldRejectLoginWhenEmailNotFound() {

        LoginRequest request = LoginRequest.builder()
                .email("unknown@gmail.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.login(request)
        )
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid Email or Password");

        verify(userRepository)
                .findByEmail("unknown@gmail.com");

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtUtil, never())
                .generateToken(anyString(), anyString());
    }


    @Test
    void shouldRejectLoginWhenPasswordIsIncorrect() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("rahul@gmail.com")
                .password("encodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("rahul@gmail.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"
        ))
                .thenReturn(false);

        assertThatThrownBy(() ->
                userService.login(request)
        )
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid Email or Password");

        verify(userRepository)
                .findByEmail("rahul@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "wrongPassword",
                        "encodedPassword"
                );

        verify(jwtUtil, never())
                .generateToken(anyString(), anyString());
    }




    @Test
    void shouldGetCurrentUserSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("rahul@gmail.com")
                .password("encodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getCurrentUser("rahul@gmail.com");

        assertThat(response).isNotNull();
        assertThat(response.getId())
                .isEqualTo(userId);
        assertThat(response.getEmail())
                .isEqualTo("rahul@gmail.com");
        assertThat(response.getFirstName())
                .isEqualTo("Rahul");
        assertThat(response.getLastName())
                .isEqualTo("Kushwaha");
        assertThat(response.getRole())
                .isEqualTo(Role.CUSTOMER);

        verify(userRepository)
                .findByEmail("rahul@gmail.com");
    }


    @Test
    void shouldThrowExceptionWhenCurrentUserNotFound() {

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getCurrentUser("unknown@gmail.com")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository)
                .findByEmail("unknown@gmail.com");
    }


    // =========================================================
    // UPDATE PROFILE
    // =========================================================

    @Test
    void shouldUpdateProfileSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("rahul@gmail.com")
                .password("encodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .firstName("Rahul Kumar")
                        .lastName("Kushwaha")
                        .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                userService.updateProfile(
                        "rahul@gmail.com",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getId())
                .isEqualTo(userId);
        assertThat(response.getEmail())
                .isEqualTo("rahul@gmail.com");
        assertThat(response.getFirstName())
                .isEqualTo("Rahul Kumar");
        assertThat(response.getLastName())
                .isEqualTo("Kushwaha");

        verify(userRepository)
                .findByEmail("rahul@gmail.com");

        verify(userRepository)
                .save(user);
    }


    @Test
    void shouldThrowExceptionWhenUpdatingProfileForUnknownUser() {

        UpdateProfileRequest request =
                UpdateProfileRequest.builder()
                        .firstName("Rahul")
                        .lastName("Kushwaha")
                        .build();

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateProfile(
                        "unknown@gmail.com",
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Enter correct email");

        verify(userRepository)
                .findByEmail("unknown@gmail.com");

        verify(userRepository, never())
                .save(any(User.class));
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Test
    void shouldChangePasswordSuccessfully() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("rahul@gmail.com")
                .password("oldEncodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .oldPassword("oldPassword")
                        .newPassword("newPassword")
                        .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "oldPassword",
                "oldEncodedPassword"
        ))
                .thenReturn(true);

        when(passwordEncoder.encode("newPassword"))
                .thenReturn("newEncodedPassword");

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                userService.changePassword(
                        "rahul@gmail.com",
                        request
                );

        assertThat(response).isNotNull();
        assertThat(response.getId())
                .isEqualTo(userId);
        assertThat(response.getEmail())
                .isEqualTo("rahul@gmail.com");

        assertThat(user.getPassword())
                .isEqualTo("newEncodedPassword");

        verify(userRepository)
                .findByEmail("rahul@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "oldPassword",
                        "oldEncodedPassword"
                );

        verify(passwordEncoder)
                .encode("newPassword");

        verify(userRepository)
                .save(user);
    }


    @Test
    void shouldRejectPasswordChangeWhenOldPasswordIsIncorrect() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("rahul@gmail.com")
                .password("oldEncodedPassword")
                .firstName("Rahul")
                .lastName("Kushwaha")
                .role(Role.CUSTOMER)
                .build();

        ChangePasswordRequest request =
                ChangePasswordRequest.builder()
                        .oldPassword("wrongOldPassword")
                        .newPassword("newPassword")
                        .build();

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongOldPassword",
                "oldEncodedPassword"
        ))
                .thenReturn(false);

        assertThatThrownBy(() ->
                userService.changePassword(
                        "rahul@gmail.com",
                        request
                )
        )
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Old password is incorrect!");

        verify(userRepository)
                .findByEmail("rahul@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "wrongOldPassword",
                        "oldEncodedPassword"
                );

        verify(passwordEncoder, never())
                .encode("newPassword");

        verify(userRepository, never())
                .save(any(User.class));
    }
}