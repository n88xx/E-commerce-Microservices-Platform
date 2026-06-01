package com.innowise.authservice.service;

import com.innowise.authservice.model.dto.AuthResponse;
import com.innowise.authservice.model.dto.LoginRequest;
import com.innowise.authservice.model.dto.RefreshRequest;
import com.innowise.authservice.model.dto.RegisterRequest;
import com.innowise.authservice.model.dto.ValidateResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    ValidateResponse validate(String token);
}