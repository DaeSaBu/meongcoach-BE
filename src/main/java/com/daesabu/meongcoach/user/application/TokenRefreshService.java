package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리프레시 토큰 rotation 재발급. 서명이 유효해도 저장 이력이 없거나 이미 폐기·만료된 토큰이면 거부하고,
 * 재발급할 때는 제시된 토큰을 폐기한 뒤 새 토큰 쌍을 발급·저장한다. 폐기와 새 발급은 한 트랜잭션이라
 * 중간에 실패하면 기존 토큰도 그대로 남는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRefreshService implements TokenRefresher {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final RegisteredUserChecker registeredUserChecker;
	private final AuthTokenIssueService authTokenIssueService;

	@Override
	@Transactional
	public AuthToken refresh(String refreshToken) {
		String tokenId = tokenProvider.extractTokenId(refreshToken);
		RefreshToken stored = refreshTokenRepository.findByTokenId(tokenId)
				.orElseThrow(InvalidRefreshTokenException::new);
		LocalDateTime now = LocalDateTime.now();
		if (!stored.isUsable(now)) {
			throw new InvalidRefreshTokenException();
		}
		// 회원은 JWT의 sub가 아니라 저장된 토큰의 소유자로 정한다. 없는 회원에게 새 토큰을 내주지 않는다
		User user = stored.getUser();
		if (!registeredUserChecker.isRegistered(user.getId())) {
			throw new InvalidRefreshTokenException();
		}
		stored.revoke();
		return authTokenIssueService.issue(user);
	}
}
