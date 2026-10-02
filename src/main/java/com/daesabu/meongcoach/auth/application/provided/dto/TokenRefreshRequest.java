package com.daesabu.meongcoach.auth.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {
}
