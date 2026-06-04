//package com.ygc.security;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import static org.assertj.core.api.Assertions.*;
//
//class JwtUtilTest {
//
//    private JwtUtil jwtUtil;
//
//    @BeforeEach
//    void setUp() {
//        jwtUtil = new JwtUtil();
//        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-testing-minimum-length-requirement-met");
//        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
//    }
//
//    @Test
//    @DisplayName("should generate and validate token")
//    void shouldGenerateAndValidateToken() {
//        String token = jwtUtil.generateToken("test@example.com");
//
//        assertThat(token).isNotNull().isNotEmpty();
//        assertThat(jwtUtil.extractUsername(token)).isEqualTo("test@example.com");
//        assertThat(jwtUtil.isTokenValid(token, "test@example.com")).isTrue();
//    }
//
//    @Test
//    @DisplayName("should reject token for different user")
//    void shouldRejectTokenForDifferentUser() {
//        String token = jwtUtil.generateToken("user1@test.com");
//        assertThat(jwtUtil.isTokenValid(token, "user2@test.com")).isFalse();
//    }
//
//    @Test
//    @DisplayName("should extract correct username from token")
//    void shouldExtractUsername() {
//        String token = jwtUtil.generateToken("admin@ygc.com");
//        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin@ygc.com");
//    }
//
//    @Test
//    @DisplayName("should handle invalid token gracefully")
//    void shouldHandleInvalidToken() {
//        assertThatThrownBy(() -> jwtUtil.extractUsername("invalid.token.here"))
//                .isInstanceOf(Exception.class);
//    }
//}
