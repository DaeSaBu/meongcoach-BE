package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 리프레시 토큰을 저장하지 않는 무상태 정책이라 DB를 조회하지 않고 서명 검증만으로 재발급한다.
 * 따라서 발급된 토큰은 만료 전까지 강제로 무효화할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class TokenRefreshService implements TokenRefresher {

	private final TokenProvider tokenProvider;

	@Override
	public AuthToken refresh(String refreshToken) {
		return tokenProvider.issue(tokenProvider.extractUserId(refreshToken));
	}
}
