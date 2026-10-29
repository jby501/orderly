package com.orderly.user.dto;

public record UserMeResponse(
        Long id,
        String email,
        String name
) {
}