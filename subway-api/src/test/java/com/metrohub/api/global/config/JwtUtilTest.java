package com.metrohub.api.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    // 32바이트 이상 Base64 인코딩된 테스트 시크릿
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3Rpbmctb25seS0zMmJ5dGVz";
    private static final long EXPIRATION = 86400000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("토큰 생성 후 이메일 추출 성공")
    void generateAndExtract() {
        String token = jwtUtil.generateToken("user@test.com");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void isTokenValid_validToken_true() {
        String token = jwtUtil.generateToken("user@test.com");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 검증 실패")
    void isTokenValid_tamperedToken_false() {
        String token = jwtUtil.generateToken("user@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void isTokenValid_expiredToken_false() {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L);
        String token = shortLived.generateToken("user@test.com");

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰 검증 실패")
    void isTokenValid_emptyToken_false() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }
}
