package com.fintech.core.security;

import com.fintech.core.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "JWT_SECRETE",
                "test-jwt-secret-key-at-least-32-bytes-long!!");
        ReflectionTestUtils.setField(tokenService, "EXPIRATION_TIME", 3_600_000L);
        ReflectionTestUtils.invokeMethod(tokenService, "init");

        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("x")
                .active(true)
                .tokenVersion(0L)
                .build();
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenSubjectMatchesAndNotExpired() {
        String token = tokenService.generateToken(user);
        AuthUser authUser = AuthUser.builder().user(user).build();
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        assertTrue(tokenService.isTokenValid(token, authUser));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenSubjectMismatch() {
        String token = tokenService.generateToken(user);
        User other = User.builder()
                .id(2L)
                .email("other@test.com")
                .password("x")
                .active(true)
                .tokenVersion(0L)
                .build();
        AuthUser otherAuth = AuthUser.builder().user(other).build();
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        assertFalse(tokenService.isTokenValid(token, otherAuth));
    }

    @Test
    void isTokenValid_shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(tokenService, "EXPIRATION_TIME", -1_000L);
        String expiredToken = tokenService.generateToken(user);
        AuthUser authUser = AuthUser.builder().user(user).build();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> tokenService.isTokenValid(expiredToken, authUser));
    }
}
