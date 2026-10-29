package com.orderly.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSubjectValidatorTest {

    private JwtSubjectValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JwtSubjectValidator();
    }

    @Test
    void 양수인_사용자_ID는_검증에_성공한다() {
        assertThat(validate("1").hasErrors()).isFalse();
        assertThat(validate("9223372036854775807").hasErrors()).isFalse();
    }

    @Test
    void 사용자_ID가_0이거나_음수이면_검증에_실패한다() {
        assertThat(validate("0").hasErrors()).isTrue();
        assertThat(validate("-1").hasErrors()).isTrue();
    }

    @Test
    void 사용자_ID가_숫자가_아니면_검증에_실패한다() {
        assertThat(validate("abc").hasErrors()).isTrue();
    }

    @Test
    void 사용자_ID가_Long_범위를_초과하면_검증에_실패한다() {
        assertThat(validate("9223372036854775808").hasErrors()).isTrue();
    }

    @Test
    void 사용자_ID가_없으면_검증에_실패한다() {
        assertThat(validate(null).hasErrors()).isTrue();
    }

    private OAuth2TokenValidatorResult validate(String subject) {
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();

        claims.put("iss", "orderly-test");

        if (subject != null) {
            claims.put("sub", subject);
        }

        Jwt jwt = new Jwt(
                "test-token",
                now,
                now.plusSeconds(1800),
                Map.of("alg", "HS256"),
                claims
        );

        return validator.validate(jwt);
    }
}