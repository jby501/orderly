package com.orderly.user;

import com.orderly.user.dto.UserSignupRequest;
import com.orderly.user.dto.UserSignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.orderly.user.dto.UserMeResponse;

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

        // 2. 비밀번호 정책 검증
        PasswordPolicy.validate(request.password());

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

    @Transactional(readOnly = true)
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

}
