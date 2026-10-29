package com.orderly.auth;

import com.orderly.auth.dto.LoginRequest;
import com.orderly.auth.dto.LoginResponse;
import com.orderly.auth.jwt.JwtProperties;
import com.orderly.auth.jwt.JwtTokenProvider;
import com.orderly.user.User;
import com.orderly.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public Long authenticate(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user.getId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Long userId = authenticate(request);

        String accessToken =
                jwtTokenProvider.generateAccessToken(userId);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtProperties.accessTokenExpiration().toSeconds()
        );
    }
}