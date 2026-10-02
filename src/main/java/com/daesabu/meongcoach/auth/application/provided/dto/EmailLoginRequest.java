package com.daesabu.meongcoach.auth.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
		@NotBlank String email,
		@NotBlank String password
) {
}
