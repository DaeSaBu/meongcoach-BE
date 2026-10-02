package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.RefreshTokenFinder;
import com.daesabu.meongcoach.auth.application.provided.RefreshTokenRegister;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenFindRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRegisterRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenRevokeRequest;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RefreshTokenModifyService implements RefreshTokenRegister {
	private final RefreshTokenRepository refreshTokenRepository;
	private final TokenProvider tokenProvider;
	private final RefreshTokenFinder refreshTokenFinder;

	@Override
	@Transactional
	public void register(Long userId, RefreshTokenRegisterRequest refreshTokenRegisterRequest) {
		RefreshToken refreshToken = RefreshToken.register(userId, refreshTokenRegisterRequest.toCommand());
		refreshTokenRepository.save(refreshToken);
	}

	@Override
	@Transactional
	public void revoke(RefreshTokenRevokeRequest refreshTokenRevokeRequest) {
		RefreshTokenId tokenId = tokenProvider.extractTokenId(refreshTokenRevokeRequest.refreshToken());

		RefreshToken refreshToken = refreshTokenFinder.findByTokenId(new RefreshTokenFindRequest(tokenId));
		refreshToken.revoke();

		refreshTokenRepository.save(refreshToken);
	}

	@Override
	@Transactional
	public void revokeAllByUserId(Long userId) {
		refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
	}
}
