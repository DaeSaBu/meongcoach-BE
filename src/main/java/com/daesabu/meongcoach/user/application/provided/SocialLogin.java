package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.SocialProvider;

public interface SocialLogin {

	/**
	 * 소셜 제공자가 발급한 자격증명을 검증하고 회원을 조회·생성한 뒤 우리 서비스 토큰을 발급한다.
	 *
	 * @param credential 소셜 제공자가 발급한 ID 토큰(OIDC)
	 */
	SocialLoginResult login(SocialProvider provider, String credential);
}
