package com.daesabu.meongcoach.user.application.required;

import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;

/**
 * 소셜 제공자가 발급한 자격증명을 검증하고 연동에 필요한 계정 정보를 읽어오는 외부 자원.
 * 제공자마다 구현체를 하나씩 두며, 검증 방식(REST 조회 / ID 토큰 서명 검증)은 구현체가 정한다.
 */
public interface SocialProfileReader {

	SocialProvider provider();

	/**
	 * @param credential 카카오는 액세스 토큰, 구글·애플은 ID 토큰
	 * @throws com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException 자격증명이 유효하지 않은 경우
	 * @throws com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException 다른 앱에서 발급된 경우
	 */
	SocialAccountLinkCommand read(String credential);
}
