package com.daesabu.meongcoach.auth.domain;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RefreshTokenRegisterCommand(
		@NotNull RefreshTokenId tokenId,
		@NotNull LocalDateTime expiresAt
) {
}
