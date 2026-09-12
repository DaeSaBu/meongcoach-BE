package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;

public interface TokenProvider {

	/**
	 * 액세스·리프레시 토큰 쌍을 발급한다. 리프레시 토큰의 jti와 만료 시각을 함께 돌려주어 호출자가 저장할 수 있게 한다.
	 *
	 * @param email 액세스 토큰에 실을 이메일. 제공자가 내려주지 않아 계정에 이메일이 없는 회원은 null이다
	 */
	AuthToken issue(Long userId, String email);

	/**
	 * 리프레시 토큰의 서명·만료·용도를 검증하고 jti를 꺼낸다.
	 *
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException 검증에 실패한 경우
	 */
	RefreshTokenId extractTokenId(String refreshToken);
}
