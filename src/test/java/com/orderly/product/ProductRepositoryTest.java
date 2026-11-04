package com.orderly.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ProductRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    ProductRepository productRepository;

    @Test
    void 상품을_저장하고_조회할_수_있다() {
        // given
        Product product = new Product(
                "무선 키보드",
                "블루투스 무선 키보드",
                new BigDecimal("89000.00"),
                100
        );

        // when
        Product savedProduct = productRepository.saveAndFlush(product);

        Product foundProduct = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        // then
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(foundProduct.getName()).isEqualTo("무선 키보드");
        assertThat(foundProduct.getDescription()).isEqualTo("블루투스 무선 키보드");
        assertThat(foundProduct.getPrice()).isEqualByComparingTo("89000.00");
        assertThat(foundProduct.getStockQuantity()).isEqualTo(100);
        assertThat(foundProduct.getCreatedAt()).isNotNull();
        assertThat(foundProduct.getUpdatedAt()).isNotNull();
    }

    @Test
    void 상품_가격은_음수일_수_없다() {
        assertThatThrownBy(() -> new Product(
                "잘못된 상품",
                "음수 가격",
                new BigDecimal("-1000"),
                10
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 가격은 0 이상이어야 합니다.");
    }

    @Test
    void 상품_재고는_음수일_수_없다() {
        assertThatThrownBy(() -> new Product(
                "잘못된 상품",
                "음수 재고",
                new BigDecimal("1000"),
                -1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고 수량은 0 이상이어야 합니다.");
    }

    @Test
    void 상품명은_비어_있을_수_없다() {
        assertThatThrownBy(() -> new Product(
                " ",
                "상품명 검증",
                new BigDecimal("1000"),
                10
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품명은 1~150자 이내여야 합니다.");
    }
}