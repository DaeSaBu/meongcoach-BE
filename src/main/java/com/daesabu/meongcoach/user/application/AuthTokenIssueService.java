package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 쌍 발급의 단일 창구. JWT 발급과 리프레시 토큰 행 저장을 항상 함께 수행해,
 * 로그인·재발급 어느 경로로 발급된 리프레시 토큰이든 재발급 시 저장 이력으로 확인할 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenIssueService {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public AuthToken issue(User user) {
		AuthToken token = tokenProvider.issue(user.getId());
		RefreshToken refreshToken = RefreshToken.issue(user, token.refreshTokenId(), token.refreshTokenExpiresAt());
		refreshTokenRepository.save(refreshToken);
		return token;
	}
}
