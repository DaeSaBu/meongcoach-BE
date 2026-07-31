package com.daesabu.meongcoach.shared.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JWT의 용도 클레임이 기대한 값인지 검증한다. 이 검증이 없으면 수명이 긴 리프레시 토큰을
 * 액세스 토큰 자리에 그대로 제출할 수 있다.
 */
public class TokenTypeValidator implements OAuth2TokenValidator<Jwt> {

	private static final String ERROR_CODE = "invalid_token";
	private static final String ERROR_DESCRIPTION = "토큰 용도가 올바르지 않습니다.";

	private final TokenType expected;

	public TokenTypeValidator(TokenType expected) {
		this.expected = expected;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (expected.claimValue().equals(token.getClaimAsString(TokenType.CLAIM_NAME))) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, ERROR_DESCRIPTION, null));
	}
}
