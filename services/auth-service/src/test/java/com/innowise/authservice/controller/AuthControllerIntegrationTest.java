package com.innowise.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.authservice.exception.LoginException;
import com.innowise.authservice.exception.RegisterException;
import com.innowise.authservice.exception.TokenException;
import com.innowise.authservice.dto.AuthResponse;
import com.innowise.authservice.dto.LoginRequest;
import com.innowise.authservice.dto.RefreshRequest;
import com.innowise.authservice.dto.RegisterRequest;
import com.innowise.authservice.dto.ValidateResponse;
import com.innowise.authservice.entity.Role;
import com.innowise.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String LOGIN = "testuser";
    private static final String PASSWORD = "password123";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh.token.value";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshRequest refreshRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setLogin(LOGIN);
        registerRequest.setPassword(PASSWORD);
        registerRequest.setUserId(USER_ID);

        loginRequest = new LoginRequest();
        loginRequest.setLogin(LOGIN);
        loginRequest.setPassword(PASSWORD);

        refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(REFRESH_TOKEN);

        authResponse = new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN);
    }

    @Test
    @DisplayName("POST /auth/register: returns 201 with token pair on valid request")
    void register_returns201WithTokens_onValidRequest() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register: returns 400 on validation failure")
    void register_returns400_onValidationFailure() throws Exception {
        RegisterRequest invalid = new RegisterRequest();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register: returns 409 when login already exists")
    void register_returns409_whenLoginAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RegisterException("Login already exists: " + LOGIN));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Login already exists: " + LOGIN));
    }

    @Test
    @DisplayName("POST /auth/login: returns 200 with token pair on valid credentials")
    void login_returns200WithTokens_onValidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));
    }

    @Test
    @DisplayName("POST /auth/login: returns 400 on blank login")
    void login_returns400_onBlankLogin() throws Exception {
        LoginRequest invalid = new LoginRequest();
        invalid.setPassword(PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login: returns 401 on bad credentials")
    void login_returns401_onBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new LoginException("Invalid login or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /auth/refresh: returns 200 with new token pair on valid refresh token")
    void refresh_returns200WithTokens_onValidRefreshToken() throws Exception {
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN));
    }

    @Test
    @DisplayName("POST /auth/refresh: returns 401 when token is invalid")
    void refresh_returns401_whenTokenIsInvalid() throws Exception {
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new TokenException("Invalid or expired token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh: returns 400 on blank refresh token")
    void refresh_returns400_onBlankRefreshToken() throws Exception {
        RefreshRequest invalid = new RefreshRequest();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /auth/validate: returns 200 with userId and role on valid token")
    void validate_returns200WithUserIdAndRole_onValidToken() throws Exception {
        when(authService.validate(ACCESS_TOKEN))
                .thenReturn(new ValidateResponse(USER_ID, Role.USER.name()));

        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.role").value(Role.USER.name()));

        verify(authService).validate(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("GET /auth/validate: returns 401 when token is invalid")
    void validate_returns401_whenTokenIsInvalid() throws Exception {
        when(authService.validate(ACCESS_TOKEN))
                .thenThrow(new TokenException("Invalid or expired token"));

        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isUnauthorized());
    }
}