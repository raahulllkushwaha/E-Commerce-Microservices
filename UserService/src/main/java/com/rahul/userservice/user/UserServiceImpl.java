package com.rahul.userservice.user;

import com.rahul.userservice.auth.JwtUtil;
import com.rahul.userservice.common.exception.InvalidCredentialsException;
import com.rahul.userservice.common.exception.ResourceNotFoundException;
import com.rahul.userservice.common.exception.UserAlreadyExistsException;
import com.rahul.userservice.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);


        return UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid Email or Password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid Email or Password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .build();

    }

    @Override
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Enter correct email"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User updatedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .role(updatedUser.getRole())
                .createdAt(updatedUser.getCreatedAt())
                .build();
    }
}
