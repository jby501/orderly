
package com.orderly.product;

import com.orderly.user.User;
import com.orderly.user.UserRepository;
import com.orderly.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper; // [추가]
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test") // [추가] 테스트 전용 설정 활성화\
class ProductControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAllInBatch();
    }

    @Test
    void 관리자는_상품을_등록할_수_있다() throws Exception {
        // given
        User admin = createUser(UserRole.ADMIN);
        String token = createToken(admin.getId());

        String requestBody = """
                {
                    "name": "무선 키보드",
                    "description": "블루투스 무선 키보드",
                    "price": 89000,
                    "stockQuantity": 100
                }
                """;

        // when & then
        String responseBody = mockMvc.perform(
                        post("/api/v1/products")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("무선 키보드"))
                .andExpect(jsonPath("$.price").value(89000))
                .andExpect(jsonPath("$.stockQuantity").value(100))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long productId = objectMapper.readTree(responseBody)
                .get("id")
                .asLong();

        Product savedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertThat(savedProduct.getName())
                .isEqualTo("무선 키보드");

        assertThat(savedProduct.getDescription())
                .isEqualTo("블루투스 무선 키보드");

        assertThat(savedProduct.getPrice())
                .isEqualByComparingTo("89000");

        assertThat(savedProduct.getStockQuantity())
                .isEqualTo(100);
    }

    @Test
    void 일반_사용자는_상품을_등록할_수_없다() throws Exception {
        // given
        User user = createUser(UserRole.USER);
        String token = createToken(user.getId());

        String requestBody = """
                {
                    "name": "무선 마우스",
                    "price": 30000,
                    "stockQuantity": 10
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/v1/products")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증되지_않은_사용자는_상품을_등록할_수_없다() throws Exception {
        String requestBody = """
                {
                    "name": "무선 마우스",
                    "price": 30000,
                    "stockQuantity": 10
                }
                """;

        mockMvc.perform(
                        post("/api/v1/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 상품_가격이_음수이면_등록할_수_없다() throws Exception {
        // given
        User admin = createUser(UserRole.ADMIN);
        String token = createToken(admin.getId());

        String requestBody = """
                {
                    "name": "잘못된 상품",
                    "price": -1000,
                    "stockQuantity": 10
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/v1/products")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void 상품_재고가_음수이면_등록할_수_없다() throws Exception {
        // given
        User admin = createUser(UserRole.ADMIN);
        String token = createToken(admin.getId());

        String requestBody = """
                {
                    "name": "잘못된 상품",
                    "price": 10000,
                    "stockQuantity": -5
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/v1/products")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증_없이_상품을_조회할_수_있다() throws Exception {
        // given
        Product product = productRepository.saveAndFlush(
                new Product(
                        "무선 키보드",
                        "블루투스 무선 키보드",
                        new BigDecimal("89000.00"),
                        100
                )
        );

        // when & then
        mockMvc.perform(
                        get("/api/v1/products/{productId}", product.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("무선 키보드"))
                .andExpect(jsonPath("$.description").value("블루투스 무선 키보드"))
                .andExpect(jsonPath("$.price").value(89000))
                .andExpect(jsonPath("$.stockQuantity").value(100));
    }

    @Test
    void 존재하지_않는_상품을_조회하면_404를_반환한다() throws Exception {
        // given
        Long nonexistentProductId = Long.MAX_VALUE;

        // when & then
        mockMvc.perform(
                        get("/api/v1/products/{productId}", nonexistentProductId)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void 상품_목록을_페이지_단위로_조회할_수_있다() throws Exception {
        // given
        for (int i = 1; i <= 25; i++) {
            productRepository.save(
                    new Product(
                            "상품 " + i,
                            "테스트 상품",
                            new BigDecimal("10000.00"),
                            100
                    )
            );
        }

        productRepository.flush();

        // when & then - 첫 번째 페이지
        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].name").value("상품 1"));

        // when & then - 두 번째 페이지
        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNext").value(true));

        // when & then - 마지막 페이지
        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "2")
                                .param("size", "10")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.content[4].name").value("상품 25"));
    }

    @Test
    void 요청한_페이지_크기가_최댓값을_초과하면_100으로_제한된다() throws Exception {
        // when & then
        mockMvc.perform(
                        get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "1000")
                )
                .andDo(print()) // [추가] 실제 요청과 응답 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
    private User createUser(UserRole role) {
        String suffix = UUID.randomUUID().toString();

        User user = userRepository.saveAndFlush(
                new User(
                        role.name().toLowerCase() + "-"
                                + suffix + "@example.com",
                        "encodedPassword",
                        "테스트 사용자"
                )
        );

        if (role == UserRole.ADMIN) {
            jdbcTemplate.update(
                    "UPDATE users SET role = ? WHERE id = ?",
                    UserRole.ADMIN.name(),
                    user.getId()
            );
        }

        return user;
    }

    private String createToken(Long userId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
