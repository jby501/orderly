package com.orderly.global.dto;

public record ErrorResponse(
        String code,
        String message
) {
}