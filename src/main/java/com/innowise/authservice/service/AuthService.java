package com.innowise.authservice.service;

import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.RefreshRequest;
import com.innowise.authservice.dto.RegisterRequest;
import com.innowise.authservice.dto.ValidateResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    ValidateResponse validate(String token);
}