package com.daesabu.meongcoach.user.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {
}
