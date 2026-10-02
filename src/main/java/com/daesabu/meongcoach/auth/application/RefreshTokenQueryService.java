package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.RefreshTokenFinder;
import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenFindRequest;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RefreshTokenQueryService implements RefreshTokenFinder {
	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	public RefreshToken findByTokenId(RefreshTokenFindRequest refreshTokenFindRequest) {
		return refreshTokenRepository.findByTokenId(refreshTokenFindRequest.tokenId())
				.orElseThrow(InvalidRefreshTokenException::new);
	}
}
