package com.daesabu.meongcoach.auth.domain;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialAccountRegisterCommand(
		@NotNull SocialProvider provider,
		@NotBlank String providerId,
		@NotNull Email email) {
}
