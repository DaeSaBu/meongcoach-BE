package com.daesabu.meongcoach.shared.webapi;

import com.daesabu.meongcoach.shared.exception.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 예외를 RFC 9457 Problem Details 응답으로 변환하는 전역 예외 처리기. 프레임워크 예외(400/404/405/415 등)는 부모 클래스가 처리하고, 도메인 예외와 예상치 못한 예외만
 * 여기에서 직접 처리한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String PROPERTY_CODE = "code";
	private static final String PROPERTY_TIMESTAMP = "timestamp";
	private static final String PROPERTY_ERRORS = "errors";
	private static final String INTERNAL_ERROR_CODE = "INTERNAL_SERVER_ERROR";
	private static final String INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";
	// 인증 실패 원인을 그대로 노출하면 공격자에게 힌트가 되므로 일반화된 문구만 응답한다
	private static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다.";
	private static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";

	@ExceptionHandler(DomainException.class)
	ProblemDetail handleDomainException(DomainException e) {
		HttpStatus status = HttpStatus.valueOf(e.getErrorCode().status());
		if (status.is5xxServerError()) {
			log.error("도메인 예외(5xx): code={}", e.getErrorCode().code(), e);
		} else {
			log.warn("도메인 예외(4xx): code={}, message={}", e.getErrorCode().code(), e.getMessage());
		}
		return problemDetail(status, e.getErrorCode().code(), e.getMessage());
	}

	// 시큐리티 필터 체인에서 던져진 예외가 SecurityExceptionTranslator를 거쳐 여기로 들어온다
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthenticationException(AuthenticationException e) {
		log.warn("인증 실패: message={}", e.getMessage());
		return problemDetail(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.name(), UNAUTHORIZED_MESSAGE);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDeniedException(AccessDeniedException e) {
		log.warn("접근 권한 없음: message={}", e.getMessage());
		return problemDetail(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.name(), FORBIDDEN_MESSAGE);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpectedException(Exception e) {
		log.error("예상치 못한 예외가 발생했습니다", e);
		return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
	                                                              HttpHeaders headers, HttpStatusCode status,
	                                                              WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of(
						"field", error.getField(),
						"message", String.valueOf(error.getDefaultMessage())
				))
				.toList();
		ProblemDetail body = ex.getBody();
		body.setProperty(PROPERTY_ERRORS, errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
	                                                         HttpHeaders headers, HttpStatusCode statusCode,
	                                                         WebRequest request) {
		log.warn("요청 처리 실패: status={}, message={}", statusCode.value(), ex.getMessage());
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
	                                                      HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail detail) {
			detail.setProperty(PROPERTY_CODE, HttpStatus.valueOf(statusCode.value()).name());
			detail.setProperty(PROPERTY_TIMESTAMP, Instant.now());
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private ProblemDetail problemDetail(HttpStatus status, String code, String detail) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setProperty(PROPERTY_CODE, code);
		problemDetail.setProperty(PROPERTY_TIMESTAMP, Instant.now());
		return problemDetail;
	}
}
