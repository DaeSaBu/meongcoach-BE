package com.daesabu.meongcoach.user.application.provided;

public interface Logout {

	/**
	 * 리프레시 토큰을 폐기한다. 이미 폐기된 토큰은 그대로 두어 같은 요청을 반복해도 결과가 같다.
	 *
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException 서명 검증에 실패했거나 저장 이력이 없는 경우
	 */
	void logout(String refreshToken);
}
