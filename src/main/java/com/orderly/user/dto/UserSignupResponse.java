package com.orderly.user.dto;

public record UserSignupResponse(
        Long id,
        String email,
        String name
) {
}