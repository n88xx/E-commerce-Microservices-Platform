package com.innowise.authservice.security;

import com.innowise.authservice.config.JwtConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtProviderTests {
    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-for-junit-testing!!".getBytes());
    private static final Long USER_ID = 42L;
    private static final String ROLE = "ROLE_USER";
    private static final long ACCESS_EXPIRATION = 3_600_000L;
    private static final long REFRESH_EXPIRATION = 86_400_000L;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn(SECRET);
        lenient().when(jwtConfig.getAccessExpiration()).thenReturn(ACCESS_EXPIRATION);
        lenient().when(jwtConfig.getRefreshExpiration()).thenReturn(REFRESH_EXPIRATION);
    }

    @Test
    void generateAccessToken_returnsNotNull() {
        String token = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateAccessToken_containsCorrectUserId() {
        String token = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertEquals(USER_ID, jwtProvider.getUserIdFromToken(token));
    }

    @Test
    void generateAccessToken_containsCorrectRole() {
        String token = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertEquals(ROLE, jwtProvider.getRoleFromToken(token));
    }


    @Test
    void generateRefreshToken_returnsNotNull() {
        String token = jwtProvider.generateRefreshToken(USER_ID);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateRefreshToken_containsCorrectUserId() {
        String token = jwtProvider.generateRefreshToken(USER_ID);

        assertEquals(USER_ID, jwtProvider.getUserIdFromToken(token));
    }

    @Test
    void generateRefreshToken_doesNotContainRoleClaim() {
        String token = jwtProvider.generateRefreshToken(USER_ID);

        assertNull(jwtProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    void validateToken_corruptedString_returnsFalse() {
        assertFalse(jwtProvider.validateToken("this.is.not.a.jwt"));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        when(jwtConfig.getAccessExpiration()).thenReturn(-1000L);
        String expiredToken = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertFalse(jwtProvider.validateToken(expiredToken));
    }

    @Test
    void getClaimsFromToken_returnsCorrectUserIdAndRole() {
        String token = jwtProvider.generateAccessToken(USER_ID, ROLE);

        assertEquals(USER_ID, jwtProvider.getUserIdFromToken(token));
        assertEquals(ROLE, jwtProvider.getRoleFromToken(token));
    }
}