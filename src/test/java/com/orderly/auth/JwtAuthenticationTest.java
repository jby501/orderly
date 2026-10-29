package com.orderly.auth;

import com.orderly.auth.jwt.JwtConfig;
import com.orderly.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.orderly.auth.jwt.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import com.orderly.auth.jwt.JwtProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(JwtAuthenticationTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.jwt.access-token-expiration=30m"
})
class JwtAuthenticationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void 토큰이_없으면_401을_반환한다() throws Exception {
        mvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 유효한_토큰이면_보호된_API에_접근한다() throws Exception {
        // given: 사용자 ID가 1인 토큰 발급
        String token = jwtTokenProvider.generateAccessToken(1L);

        // when & then: 토큰을 보내면 접근 허용
        mvc.perform(get("/test/protected")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("인증 성공"));
    }

    @Test
    void 다른_키로_서명한_토큰이면_401을_반환한다() throws Exception {
        // given: 서버에서 사용하는 것과 다른 테스트용 키 준비
        byte[] differentKeyBytes = new byte[32];
        Arrays.fill(differentKeyBytes, (byte) 1);

        String differentSecret = Base64.getEncoder()
                .encodeToString(differentKeyBytes);

        JwtProperties differentProperties = new JwtProperties(
                differentSecret,
                Duration.ofMinutes(30)
        );

        // 다른 키로 토큰을 발급할 객체 구성
        JwtConfig config = new JwtConfig();

        JwtTokenProvider differentProvider = new JwtTokenProvider(
                config.jwtEncoder(
                        config.jwtSecretKey(differentProperties)
                ),
                differentProperties
        );

        String token = differentProvider.generateAccessToken(1L);

        // when & then: 서버가 서명을 신뢰하지 않아 거절
        mvc.perform(get("/test/protected")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료된_토큰이면_401을_반환한다() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300))
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        mvc.perform(get("/test/protected")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtConfig.class,
            JwtTokenProvider.class,
            ProtectedController.class
    })
    static class TestConfig {
    }

    @RestController
    static class ProtectedController {

        @GetMapping(
                value = "/test/protected",
                produces = "text/plain;charset=UTF-8"
        )
        public String protectedEndpoint() {
            return "인증 성공";
        }
    }
}