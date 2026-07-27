package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.application.provided.AuthToken;

public interface TokenProvider {

	AuthToken issue(Long userId);

	/**
	 * 리프레시 토큰의 서명·만료·용도를 검증하고 회원 식별자를 꺼낸다.
	 *
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException 검증에 실패한 경우
	 */
	Long extractUserId(String refreshToken);
}
