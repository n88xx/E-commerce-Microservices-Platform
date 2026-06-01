package com.innowise.authservice.service.impl;

import com.innowise.authservice.exception.LoginException;
import com.innowise.authservice.exception.RegisterException;
import com.innowise.authservice.exception.TokenException;
import com.innowise.authservice.model.dto.AuthResponse;
import com.innowise.authservice.model.dto.LoginRequest;
import com.innowise.authservice.model.dto.RefreshRequest;
import com.innowise.authservice.model.dto.RegisterRequest;
import com.innowise.authservice.model.dto.ValidateResponse;
import com.innowise.authservice.model.entity.Credential;
import com.innowise.authservice.repository.CredentialRepository;
import com.innowise.authservice.service.AuthService;
import com.innowise.authservice.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CredentialRepository credentialRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (credentialRepository.existsByLogin(request.getLogin())) {
            throw new RegisterException("Login already exists: " + request.getLogin());
        }

        Credential credential = Credential.builder()
                .userId(request.getUserId())
                .login(request.getLogin())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        credentialRepository.save(credential);
        log.info("Registered new user with login: {}", request.getLogin());

        return generateTokenPair(credential);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
            );
        } catch (BadCredentialsException exception) {
            throw new LoginException("Invalid login or password");
        }

        Credential credential = credentialRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new LoginException("Credential not found"));

        log.info("User logged in: {}", request.getLogin());

        return generateTokenPair(credential);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        jwtProvider.validateToken(request.getRefreshToken());

        Long userId = jwtProvider.getUserIdFromToken(request.getRefreshToken());
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new TokenException("Credential not found for userId: " + userId));

        log.info("Tokens refreshed for userId: {}", userId);

        return generateTokenPair(credential);
    }

    @Override
    public ValidateResponse validate(String token) {
        jwtProvider.validateToken(token);

        Long userId = jwtProvider.getUserIdFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);

        return new ValidateResponse(userId, role);
    }

    private AuthResponse generateTokenPair(Credential credential) {
        String accessToken = jwtProvider.generateAccessToken(credential.getUserId(), String.valueOf(credential.getRole()));
        String refreshToken = jwtProvider.generateRefreshToken(credential.getUserId());

        return new AuthResponse(accessToken, refreshToken);
    }
}