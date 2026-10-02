package com.daesabu.meongcoach.auth.application.provided.dto;

import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.RefreshTokenRegisterCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RefreshTokenRegisterRequest(
		@NotNull RefreshTokenId refreshTokenId,
		@NotNull LocalDateTime refreshTokenExpiresAt
) {
	public RefreshTokenRegisterCommand toCommand() {
		return new RefreshTokenRegisterCommand(refreshTokenId, refreshTokenExpiresAt);
	}
}
