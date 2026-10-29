package com.orderly.auth.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtSubjectValidator
        implements OAuth2TokenValidator<Jwt> {

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject != null) {
            try {
                long userId = Long.parseLong(subject);

                if (userId > 0) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (NumberFormatException exception) {
                // 아래의 검증 실패 결과로 처리한다.
            }
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "토큰의 사용자 ID가 올바르지 않습니다.",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}