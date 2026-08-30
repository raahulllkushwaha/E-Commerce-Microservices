package com.rahul.userservice.user;

import com.rahul.userservice.user.dto.LoginRequest;
import com.rahul.userservice.user.dto.RegisterRequest;
import com.rahul.userservice.user.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);
    UserResponse login(LoginRequest request);
}
