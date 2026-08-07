package com.daesabu.meongcoach.user.adapter.security;

import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 토큰의 sub가 실제로 등록된 회원인지 검증한다. 이 검증이 없으면 회원 행이 사라진 뒤에도
 * 남아 있는 토큰이 만료 전까지 그대로 통과한다.
 * 회원 조회가 필요해 shared가 아니라 user 모듈에 두고, 액세스 토큰 디코더에만 붙인다.
 */
@Component
public class RegisteredUserValidator implements OAuth2TokenValidator<Jwt> {

	private static final String ERROR_CODE = "invalid_token";

	// 원인을 자세히 알리면 회원 ID의 존재 여부가 노출되므로 일반화된 문구만 담는다
	private static final String ERROR_DESCRIPTION = "인증 정보가 올바르지 않습니다.";

	private final RegisteredUserChecker registeredUserChecker;

	public RegisteredUserValidator(RegisteredUserChecker registeredUserChecker) {
		this.registeredUserChecker = registeredUserChecker;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Long userId = userId(token);
		if (userId != null && registeredUserChecker.isRegistered(userId)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, ERROR_DESCRIPTION, null));
	}

	// 검증기에서 던진 예외는 인증 예외로 변환되지 않아 500이 되므로, 형식 위반도 검증 실패로 돌린다
	private Long userId(Jwt token) {
		try {
			return Long.valueOf(token.getSubject());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
