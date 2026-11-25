package com.orderly.product;

public class InvalidProductSortException extends RuntimeException {

    public InvalidProductSortException() {
        super(
                "지원하지 않는 상품 정렬 항목입니다. "
                        + "id, name, price, createdAt만 사용할 수 있습니다."
        );
    }
}