package com.rahul.userservice.user;

import com.rahul.userservice.user.dto.*;

public interface UserService {

    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    UserResponse changePassword(String email, ChangePasswordRequest request);
}
