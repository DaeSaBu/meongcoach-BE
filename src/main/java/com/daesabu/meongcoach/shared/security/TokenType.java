package com.daesabu.meongcoach.shared.security;

import java.util.Locale;

/**
 * 자체 발급 JWT의 용도 구분. 액세스 토큰과 리프레시 토큰이 서로의 자리에서 쓰이지 못하도록
 * {@link #CLAIM_NAME} 클레임에 용도를 담고 검증 시 기대값과 대조한다.
 */
public enum TokenType {
	ACCESS,
	REFRESH,
	;

	public static final String CLAIM_NAME = "token_type";

	public String claimValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
