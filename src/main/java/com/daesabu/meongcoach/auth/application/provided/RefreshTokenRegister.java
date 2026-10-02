package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRegisterRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRevokeRequest;
import jakarta.validation.Valid;

public interface RefreshTokenRegister {
	void register(Long userId, @Valid RefreshTokenRegisterRequest refreshTokenRegisterRequest);

	void revoke(@Valid RefreshTokenRevokeRequest refreshTokenRevokeRequest);

	void revokeAllByUserId(Long userId);
}
