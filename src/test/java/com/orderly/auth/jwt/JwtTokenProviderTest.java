package com.orderly.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        // 테스트에서 사용할 32바이트 비밀 키 생성
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);

        // 실제 JwtConfig에서 사용하는 설정 방식과 동일하게 구성
        String encodedSecret = Base64.getEncoder()
                .encodeToString(keyBytes);

        JwtProperties properties = new JwtProperties(
                encodedSecret,
                Duration.ofMinutes(30)
        );

        JwtConfig jwtConfig = new JwtConfig();

        SecretKey signingKey = jwtConfig.jwtSecretKey(properties);

        JwtEncoder jwtEncoder = jwtConfig.jwtEncoder(signingKey);

        jwtTokenProvider = new JwtTokenProvider(
                jwtEncoder,
                properties
        );

        // 생성된 JWT를 검증할 Decoder
        SecretKey secretKey = new SecretKeySpec(
                keyBytes,
                "HmacSHA256"
        );

        jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Test
    void JWT_발급_시_사용자_ID와_만료_시간이_정확하다() {
        // given
        Long userId = 1L;

        Instant before = Instant.now();

        // when
        String token = jwtTokenProvider.generateAccessToken(userId);

        Instant after = Instant.now();

        // then
        assertThat(token).isNotBlank();

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("1");

        assertThat(jwt.getIssuedAt())
                .isBetween(before.minusSeconds(1), after.plusSeconds(1));

        assertThat(jwt.getExpiresAt())
                .isEqualTo(jwt.getIssuedAt().plus(Duration.ofMinutes(30)));

        assertThat(jwt.getHeaders().get("alg"))
                .isEqualTo("HS256");
    }

    @Test
    void 다른_서명_키로는_JWT를_검증할_수_없다() {
        // given
        String token = jwtTokenProvider.generateAccessToken(1L);

        byte[] differentKeyBytes = new byte[32];
        new SecureRandom().nextBytes(differentKeyBytes);

        SecretKey differentKey = new SecretKeySpec(
                differentKeyBytes,
                "HmacSHA256"
        );

        JwtDecoder differentDecoder = NimbusJwtDecoder
                .withSecretKey(differentKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // when & then
        assertThatThrownBy(() -> differentDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }
}