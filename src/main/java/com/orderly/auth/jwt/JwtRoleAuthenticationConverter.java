package com.orderly.auth.jwt;

import com.orderly.user.User;
import com.orderly.user.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtRoleAuthenticationConverter
        implements Converter<Jwt, JwtAuthenticationToken> {

    private final UserRepository userRepository;

    public JwtRoleAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException(
                        "인증된 사용자를 찾을 수 없습니다."
                ));

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new JwtAuthenticationToken(
                jwt,
                List.of(authority),
                jwt.getSubject()
        );
    }
}