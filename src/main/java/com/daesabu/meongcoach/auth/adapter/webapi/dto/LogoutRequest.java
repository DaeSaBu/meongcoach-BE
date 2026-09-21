package com.daesabu.meongcoach.auth.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {
}
