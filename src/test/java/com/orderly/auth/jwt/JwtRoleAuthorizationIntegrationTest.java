
package com.orderly.auth.jwt;

import com.orderly.user.User;
import com.orderly.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class JwtRoleAuthorizationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JwtEncoder jwtEncoder;

    private Long userId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        User user = userRepository.saveAndFlush(
                new User(
                        "user-" + suffix + "@example.com",
                        "encodedPassword",
                        "일반사용자"
                )
        );

        User admin = userRepository.saveAndFlush(
                new User(
                        "admin-" + suffix + "@example.com",
                        "encodedPassword",
                        "관리자"
                )
        );

        userId = user.getId();
        adminId = admin.getId();

        jdbcTemplate.update(
                "UPDATE users SET role = ? WHERE id = ?",
                "ADMIN",
                adminId
        );
    }

    @Test
    void 인증되지_않은_사용자는_관리자_API에_접근할_수_없다()
            throws Exception {

        mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void 일반_사용자는_관리자_API에_접근할_수_없다()
            throws Exception {

        String token = createToken(userId);

        mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_관리자_API의_권한_검사를_통과한다()
            throws Exception {

        String token = createToken(adminId);

        mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(result -> {
            int status = result.getResponse().getStatus();

            if (status == 401 || status == 403) {
                throw new AssertionError(
                        "관리자 요청이 인증 또는 인가 단계에서 거부되었습니다. HTTP " + status
                );
            }
        });
    }

    @Test
    void DB에_존재하지_않는_사용자는_인증되지_않는다()
            throws Exception {

        String token = createToken(Long.MAX_VALUE);

        mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isUnauthorized());
    }

    private String createToken(Long subject) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
