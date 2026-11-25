package com.orderly.product.dto;

import com.orderly.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(

        @NotNull(message = "상품 판매 상태는 필수입니다.")
        ProductStatus status

) {
}