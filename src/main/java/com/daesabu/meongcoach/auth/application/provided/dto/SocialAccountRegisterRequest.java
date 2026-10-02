package com.daesabu.meongcoach.auth.application.provided.dto;

import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.SocialAccountRegisterCommand;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialAccountRegisterRequest(
		@NotNull SocialProvider provider,
		@NotBlank String providerId,
		@NotNull Email email
) {
	public SocialAccountRegisterCommand toCommand() {
		return new SocialAccountRegisterCommand(provider, providerId, email);
	}
}
