package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AuthTokenProvider;
import com.daesabu.meongcoach.auth.application.provided.RefreshTokenRegister;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRegisterRequest;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthTokenProvideService implements AuthTokenProvider {
	private final TokenProvider tokenProvider;
	private final RefreshTokenRegister refreshTokenRegister;

	@Override
	@Transactional
	public AuthToken issue(Long userId) {
		AuthToken authToken = tokenProvider.issue(userId);

		registerRefreshToken(userId, authToken);

		return authToken;
	}

	private void registerRefreshToken(Long userId, AuthToken authToken) {
		refreshTokenRegister.register(userId,
				new RefreshTokenRegisterRequest(authToken.refreshTokenId(), authToken.refreshTokenExpiresAt()));
	}
}
