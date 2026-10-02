package com.daesabu.meongcoach.auth.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		@NotBlank String refreshToken
) {
}
