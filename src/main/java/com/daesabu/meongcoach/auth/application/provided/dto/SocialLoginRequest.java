package com.daesabu.meongcoach.auth.application.provided.dto;

import com.daesabu.meongcoach.auth.domain.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
		@NotNull SocialProvider socialProvider,
		@NotBlank String idToken
) {
}
