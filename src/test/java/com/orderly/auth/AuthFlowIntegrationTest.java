package com.orderly.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 회원가입_로그인_JWT인증_내정보조회_전체흐름() throws Exception {

        MockMvc mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";
        String name = "통합테스트";

        // 1. 회원가입
        String signupRequest = objectMapper.writeValueAsString(
                new SignupPayload(email, password, name)
        );

        String signupResponse = mvc.perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(signupRequest)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode signupJson = objectMapper.readTree(signupResponse);
        long userId = signupJson.get("id").asLong();

        // 2. 로그인
        String loginRequest = objectMapper.writeValueAsString(
                new LoginPayload(email, password)
        );

        String loginResponse = mvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);

        String accessToken = loginJson.get("accessToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(loginJson.get("tokenType").asText())
                .isEqualTo("Bearer");
        assertThat(loginJson.get("expiresIn").asLong())
                .isEqualTo(1800);

        // 3. 실제 JWT로 내 정보 조회
        mvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private record SignupPayload(
            String email,
            String password,
            String name
    ) {
    }

    private record LoginPayload(
            String email,
            String password
    ) {
    }
}