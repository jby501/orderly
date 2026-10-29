package com.orderly.user;

import com.orderly.auth.jwt.JwtConfig;
import com.orderly.config.SecurityConfig;
import com.orderly.global.exception.GlobalExceptionHandler;
import com.orderly.user.dto.UserMeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(UserMeControllerTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.jwt.access-token-expiration=30m"
})
class UserMeControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(userService);

        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 인증된_사용자의_정보를_반환한다() throws Exception {
        when(userService.getMe(2L))
                .thenReturn(new UserMeResponse(
                        2L,
                        "login-test@example.com",
                        "로그인테스트"
                ));

        mvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(builder ->
                                builder.subject("2")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email")
                        .value("login-test@example.com"))
                .andExpect(jsonPath("$.name")
                        .value("로그인테스트"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService).getMe(2L);
    }

    @Test
    void 회원이_없으면_404를_반환한다() throws Exception {
        when(userService.getMe(999L))
                .thenThrow(new UserNotFoundException());

        mvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(builder ->
                                builder.subject("999")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("회원을 찾을 수 없습니다."));

        verify(userService).getMe(999L);
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void 사용자_ID가_잘못된_JWT는_401을_반환한다() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("abc")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtConfig.class,
            UserController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfig {

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }
    }
}