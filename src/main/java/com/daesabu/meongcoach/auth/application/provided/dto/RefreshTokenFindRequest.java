package com.daesabu.meongcoach.auth.application.provided.dto;

import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import jakarta.validation.constraints.NotNull;

public record RefreshTokenFindRequest(
		@NotNull RefreshTokenId tokenId
) {
}
