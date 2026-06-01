package com.innowise.authservice.service.impl;

import com.innowise.authservice.security.JwtProvider;
import com.innowise.authservice.exception.LoginException;
import com.innowise.authservice.exception.RegisterException;
import com.innowise.authservice.exception.TokenException;
import com.innowise.authservice.model.dto.AuthResponse;
import com.innowise.authservice.model.dto.LoginRequest;
import com.innowise.authservice.model.dto.RefreshRequest;
import com.innowise.authservice.model.dto.RegisterRequest;
import com.innowise.authservice.model.dto.ValidateResponse;
import com.innowise.authservice.model.entity.Credential;
import com.innowise.authservice.model.entity.Role;
import com.innowise.authservice.repository.CredentialRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    private static final Long USER_ID = 1L;
    private static final String LOGIN = "testuser";
    private static final String PASSWORD = "password123";
    private static final String PASSWORD_HASH = "hashedPassword";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN = "refresh.token.value";
    private static final Role ROLE = Role.USER;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private Credential credential;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshRequest refreshRequest;

    @BeforeEach
    void setUp() {
        credential = Credential.builder()
                .userId(USER_ID)
                .login(LOGIN)
                .passwordHash(PASSWORD_HASH)
                .role(ROLE)
                .active(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setLogin(LOGIN);
        registerRequest.setPassword(PASSWORD);
        registerRequest.setRole(ROLE);
        registerRequest.setUserId(USER_ID);

        loginRequest = new LoginRequest();
        loginRequest.setLogin(LOGIN);
        loginRequest.setPassword(PASSWORD);

        refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("register: saves credential and returns token pair when login is unique")
    void register_savesCredentialAndReturnsTokens_whenLoginIsUnique() {
        when(credentialRepository.existsByLogin(LOGIN)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(jwtProvider.generateAccessToken(USER_ID, ROLE.name())).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());

        Credential saved = captor.getValue();
        assertThat(saved.getLogin()).isEqualTo(LOGIN);
        assertThat(saved.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getRole()).isEqualTo(ROLE);
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    @DisplayName("register: throws RegisterException when login already exists")
    void register_throwsRegisterException_whenLoginAlreadyExists() {
        when(credentialRepository.existsByLogin(LOGIN)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RegisterException.class)
                .hasMessageContaining(LOGIN);

        verify(credentialRepository, never()).save(any());
        verify(jwtProvider, never()).generateAccessToken(anyLong(), any());
    }

    @Test
    @DisplayName("login: authenticates and returns token pair on valid credentials")
    void login_authenticatesAndReturnsTokens_onValidCredentials() {
        when(credentialRepository.findByLogin(LOGIN)).thenReturn(Optional.of(credential));
        when(jwtProvider.generateAccessToken(USER_ID,ROLE.name())).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);

        verify(authenticationManager)
                .authenticate(new UsernamePasswordAuthenticationToken(LOGIN, PASSWORD));
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException from AuthenticationManager")
    void login_propagatesLoginException_fromAuthenticationManager() {
    when(authenticationManager.authenticate(any()))
        .thenThrow(new LoginException("Bad Credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LoginException.class);

        verify(credentialRepository, never()).findByLogin(anyString());
        verify(jwtProvider, never()).generateAccessToken(anyLong(), any());
    }

    @Test
    @DisplayName("login: throws LoginException when credential is missing after authentication")
    void login_throwsLoginException_whenCredentialNotFound() {
        when(credentialRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LoginException.class);
    }

    @Test
    @DisplayName("refresh: validates token and returns new token pair")
    void refresh_validatesTokenAndReturnsNewPair() {
        when(jwtProvider.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(credentialRepository.findByUserId(USER_ID)).thenReturn(Optional.of(credential));
        when(jwtProvider.generateAccessToken(USER_ID, ROLE.name())).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN);

        AuthResponse response = authService.refresh(refreshRequest);

        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(jwtProvider).validateToken(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh: throws TokenException when credential for userId is not found")
    void refresh_throwsTokenException_whenCredentialNotFound() {
        when(jwtProvider.getUserIdFromToken(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(credentialRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshRequest))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining(String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("refresh: propagates JwtException from validateToken")
    void refresh_propagatesJwtException_fromValidateToken() {
        org.mockito.Mockito.doThrow(new JwtException("Invalid token"))
                .when(jwtProvider).validateToken(REFRESH_TOKEN);

        assertThatThrownBy(() -> authService.refresh(refreshRequest))
                .isInstanceOf(JwtException.class);

        verify(credentialRepository, never()).findByUserId(anyLong());
    }

    @Test
    @DisplayName("validate: returns userId and role from token")
    void validate_returnsUserIdAndRole_fromToken() {
        when(jwtProvider.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtProvider.getRoleFromToken(ACCESS_TOKEN)).thenReturn(ROLE.name());

        ValidateResponse response = authService.validate(ACCESS_TOKEN);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getRole()).isEqualTo(ROLE.name());
        verify(jwtProvider).validateToken(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("validate: propagates JwtException from validateToken")
    void validate_propagatesJwtException_fromValidateToken() {
        org.mockito.Mockito.doThrow(new JwtException("Expired"))
                .when(jwtProvider).validateToken(ACCESS_TOKEN);

        assertThatThrownBy(() -> authService.validate(ACCESS_TOKEN))
                .isInstanceOf(JwtException.class);

        verify(jwtProvider, never()).getUserIdFromToken(ACCESS_TOKEN);
    }
}