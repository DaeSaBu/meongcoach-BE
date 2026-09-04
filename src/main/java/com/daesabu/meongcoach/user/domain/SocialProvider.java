package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import java.util.Locale;

/**
 * 소셜 로그인 제공자.
 */
public enum SocialProvider {
	KAKAO,
	GOOGLE,
	APPLE,
	;

	/**
	 * 요청 경로의 제공자 문자열을 enum으로 변환한다. 스프링 기본 변환기는 대소문자를 구분하고
	 * 실패 시 우리 에러 코드를 잃어버리므로 도메인에서 직접 변환한다.
	 */
	public static SocialProvider from(String value) {
		if (value == null || value.isBlank()) {
			throw new UnsupportedSocialProviderException(value);
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new UnsupportedSocialProviderException(value);
		}
	}
}
