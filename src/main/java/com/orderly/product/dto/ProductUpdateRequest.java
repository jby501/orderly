package com.orderly.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateRequest(

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 150, message = "상품명은 150자 이하여야 합니다.")
        String name,

        String description,

        @NotNull(message = "상품 가격은 필수입니다.")
        @DecimalMin(value = "0.0", message = "상품 가격은 0 이상이어야 합니다.")
        @Digits(integer = 13, fraction = 2,
                message = "가격은 정수 13자리, 소수 2자리 이내여야 합니다.")
        BigDecimal price

) {
}
