package com.orderly.user;

import com.orderly.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void 신규_사용자는_USER_역할을_가진다() {
        // given
        String email = "user@example.com";
        String password = "encodedPassword";
        String name = "홍길동";

        // when
        User user = new User(email, password, name);

        // then
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }
}