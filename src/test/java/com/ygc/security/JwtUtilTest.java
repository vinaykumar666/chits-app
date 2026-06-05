package com.ygc.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private UserDetails testUser;
    private UserDetails otherUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // secret must be >= 256 bits (32 bytes) for HS256
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-for-jwt-testing-minimum-32-bytes!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        testUser = new User("test@example.com", "password", Collections.emptyList());
        otherUser = new User("other@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("should generate a non-empty token")
    void shouldGenerateToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("should extract correct username from token")
    void shouldExtractUsername() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("should validate token for correct user")
    void shouldValidateTokenForCorrectUser() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.validateToken(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("should reject token for different user")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.validateToken(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("should reject malformed token")
    void shouldRejectMalformedToken() {
        assertThat(jwtUtil.validateToken("invalid.token.here", testUser)).isFalse();
    }

    @Test
    @DisplayName("should reject expired token")
    void shouldRejectExpiredToken() {
        // Set expiration to 0ms (immediately expired)
        ReflectionTestUtils.setField(jwtUtil, "expiration", 0L);
        String token = jwtUtil.generateToken(testUser);

        // Token is already expired
        assertThat(jwtUtil.validateToken(token, testUser)).isFalse();
    }
}
