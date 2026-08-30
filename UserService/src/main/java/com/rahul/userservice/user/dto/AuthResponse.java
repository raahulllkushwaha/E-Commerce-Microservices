package com.rahul.userservice.user.dto;

import com.rahul.userservice.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID id;
    private String email;
    private Role role;
    private String token;
}