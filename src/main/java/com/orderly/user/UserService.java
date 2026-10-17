package com.orderly.user;

import com.orderly.user.dto.UserSignupRequest;
import com.orderly.user.dto.UserSignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSignupResponse signup(UserSignupRequest request) {

        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        // 2. BCrypt 비밀번호 길이 검증
        validatePassword(request.password());

        // 3. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.password());

        // 4. 회원 엔티티 생성
        User user = new User(
                request.email(),
                encodedPassword,
                request.name()
        );

        // 5. 회원 저장
        User savedUser = userRepository.save(user);

        // 6. 응답 DTO 반환
        return new UserSignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName()
        );
    }

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("비밀번호는 필수입니다.");
        }

        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;

        if (byteLength < 8 || byteLength > 72) {
            throw new InvalidPasswordException(
                    "비밀번호는 UTF-8 기준 8~72바이트여야 합니다."
            );
        }
    }
}