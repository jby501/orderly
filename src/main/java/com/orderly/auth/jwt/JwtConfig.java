package com.orderly.auth.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {

        byte[] keyBytes = Base64.getDecoder()
                .decode(properties.secret());

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 서명 키는 최소 32바이트 이상이어야 합니다."
            );
        }

        SecretKey secretKey = new SecretKeySpec(
                keyBytes,
                "HmacSHA256"
        );

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }
}