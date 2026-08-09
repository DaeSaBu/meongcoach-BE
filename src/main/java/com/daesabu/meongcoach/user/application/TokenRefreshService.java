package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 리프레시 토큰을 저장하지 않는 무상태 정책이라 서명 검증으로 회원을 식별하고,
 * 그 회원이 아직 등록되어 있는지만 확인한 뒤 재발급한다.
 * 토큰 자체를 무효화하는 수단은 없으므로 발급된 토큰은 만료 전까지 되돌릴 수 없다.
 */
@Service
@RequiredArgsConstructor
public class TokenRefreshService implements TokenRefresher {

	private final TokenProvider tokenProvider;
	private final RegisteredUserChecker registeredUserChecker;

	@Override
	public AuthToken refresh(String refreshToken) {
		Long userId = tokenProvider.extractUserId(refreshToken);
		// 없는 회원에게 새 토큰을 내주지 않는다. 재로그인하면 소셜 로그인이 회원을 다시 만든다
		if (!registeredUserChecker.isRegistered(userId)) {
			throw new InvalidRefreshTokenException();
		}
		return tokenProvider.issue(userId);
	}
}
