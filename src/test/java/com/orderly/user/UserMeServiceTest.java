package com.orderly.user;

import com.orderly.user.dto.UserMeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserMeServiceTest {

    private final UserRepository userRepository =
            mock(UserRepository.class);

    private final PasswordEncoder passwordEncoder =
            mock(PasswordEncoder.class);

    private final UserService userService =
            new UserService(userRepository, passwordEncoder);

    @Test
    void 회원이_존재하면_내_정보를_반환한다() {
        // given
        Long userId = 2L;

        User user = new User(
                "login-test@example.com",
                "encoded-password",
                "로그인테스트"
        );

        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // when
        UserMeResponse response = userService.getMe(userId);

        // then
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email())
                .isEqualTo("login-test@example.com");
        assertThat(response.name())
                .isEqualTo("로그인테스트");

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void 회원이_없으면_예외가_발생한다() {
        // given
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMe(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("회원을 찾을 수 없습니다.");

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder);
    }
}