package com.orderly.auth;

import com.orderly.auth.dto.LoginRequest;
import com.orderly.user.User;
import com.orderly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private LoginRequest request;

    @BeforeEach
    void setUp() {
        request = new LoginRequest(
                "test@example.com",
                "Password123!"
        );
    }

    @Test
    void 로그인에_성공하면_회원_ID를_반환한다() {
        // given
        User user = new User(
                "test@example.com",
                "encodedPassword",
                "테스트"
        );

        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )).thenReturn(true);

        // when
        Long userId = authService.authenticate(request);

        // then
        assertThat(userId).isEqualTo(user.getId());

        verify(userRepository).findByEmail(request.email());
        verify(passwordEncoder).matches(
                request.password(),
                user.getPassword()
        );
    }

    @Test
    void 존재하지_않는_이메일이면_예외가_발생한다() {
        // given
        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void 비밀번호가_일치하지_않으면_예외가_발생한다() {
        // given
        User user = new User(
                "test@example.com",
                "encodedPassword",
                "테스트"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(passwordEncoder).matches(
                request.password(),
                user.getPassword()
        );
    }
}