package com.daesabu.meongcoach.auth.adapter.webapi.legacy;

import com.daesabu.meongcoach.auth.domain.exception.AuthErrorCode;
import com.daesabu.meongcoach.shared.exception.DomainException;
import com.daesabu.meongcoach.shared.exception.ErrorCode;
import com.daesabu.meongcoach.shared.webapi.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 구 클라이언트는 에러 응답의 code를 {@code USER_} 접두어로 분기한다(이메일 로그인 문구, Apple 재인증 후 탈퇴 재요청).
 * auth 모듈 분리로 코드가 {@code AUTH_} 접두어로 바뀌었으므로 구 경로에서만 접두어를 되돌린다.
 * 개별 예외 핸들러를 두지 않는 규칙의 의도적 예외다 — LegacyAuthController에만 적용되고, 응답 생성·로깅은 전역 핸들러에 그대로 맡기며
 * code 문자열만 바꾼다. 구 앱 지원이 끝나면 이 패키지와 함께 삭제한다.
 */
@RestControllerAdvice(assignableTypes = LegacyAuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LegacyAuthErrorCodeHandler {

	private static final String PROPERTY_CODE = "code";
	private static final String CURRENT_PREFIX = "AUTH_";
	private static final String LEGACY_PREFIX = "USER_";

	private final GlobalExceptionHandler globalExceptionHandler;

	@ExceptionHandler(DomainException.class)
	ProblemDetail handleDomainException(DomainException e) {
		ProblemDetail problemDetail = globalExceptionHandler.handleDomainException(e);
		problemDetail.setProperty(PROPERTY_CODE, legacyCode(e.getErrorCode()));
		return problemDetail;
	}

	// auth 모듈 코드만 접두어를 바꾼다. 다른 모듈 코드(USER_NOT_FOUND 등)는 분리 전과 이름이 같다
	private static String legacyCode(ErrorCode errorCode) {
		String code = errorCode.code();
		if (errorCode instanceof AuthErrorCode) {
			return LEGACY_PREFIX + code.substring(CURRENT_PREFIX.length());
		}
		return code;
	}
}
