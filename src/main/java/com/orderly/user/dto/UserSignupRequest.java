package com.orderly.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(

        @NotBlank
        @Email
        String email,

        String password,

        @NotBlank
        @Size(max = 100)
        String name

) {
}