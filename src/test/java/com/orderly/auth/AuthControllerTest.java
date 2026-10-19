package com.orderly.auth;

import com.orderly.auth.dto.LoginRequest;
import com.orderly.auth.dto.LoginResponse;
import com.orderly.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void 로그인에_성공하면_토큰을_반환한다() throws Exception {
        LoginRequest request = new LoginRequest(
                "login-test@example.com",
                "Test1234!"
        );

        when(authService.login(request))
                .thenReturn(new LoginResponse(
                        "test-access-token",
                        "Bearer",
                        1800L
                ));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-test@example.com",
                                  "password": "Test1234!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("test-access-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(1800));

        verify(authService).login(request);
    }

    @Test
    void 인증에_실패하면_401을_반환한다() throws Exception {
        LoginRequest request = new LoginRequest(
                "login-test@example.com",
                "Wrong1234!"
        );

        when(authService.login(request))
                .thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-test@example.com",
                                  "password": "Wrong1234!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message")
                        .value("이메일 또는 비밀번호가 올바르지 않습니다."));

        verify(authService).login(request);
    }

    @Test
    void 이메일_형식이_잘못되면_400을_반환한다() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "Test1234!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"));

        verifyNoInteractions(authService);
    }

    @Test
    void 비밀번호가_비어있으면_400을_반환한다() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-test@example.com",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"));

        verifyNoInteractions(authService);
    }
}