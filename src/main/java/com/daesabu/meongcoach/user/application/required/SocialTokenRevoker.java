package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.SocialProvider;

/**
 * 탈퇴 시 소셜 제공자에게 우리 앱과의 연결을 끊도록 요청하는 외부 자원.
 * 제공자마다 구현체를 하나씩 두며, 제공자와 주고받는 자격증명의 형태와 필수 여부는 구현체가 정한다.
 */
public interface SocialTokenRevoker {

	SocialProvider provider();

	/**
	 * @param authorizationCode 클라이언트가 탈퇴 직전 제공자 SDK로 새로 받은 1회용 인가 코드. null일 수 있으며,
	 *                          제공자가 코드를 필수로 요구하면 구현체가 예외를 던진다
	 * @throws com.daesabu.meongcoach.user.domain.exception.AppleAuthorizationCodeRequiredException Apple이 코드 없이 호출된 경우
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidAppleAuthorizationCodeException 제공자가 코드를 거부한 경우
	 * @throws com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException 제공자와 통신할 수 없는 경우
	 */
	void revoke(String authorizationCode);
}
