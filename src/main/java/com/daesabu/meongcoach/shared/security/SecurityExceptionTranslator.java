package com.daesabu.meongcoach.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 시큐리티 필터 체인에서 발생한 인증·인가 예외를 Spring MVC 예외 처리기로 되돌려,
 * GlobalExceptionHandler가 만드는 Problem Details 형식을 그대로 재사용한다.
 * 필터에서 던져진 예외는 DispatcherServlet 밖이라 @RestControllerAdvice가 잡지 못한다.
 */
@Slf4j
@Component
public class SecurityExceptionTranslator implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public SecurityExceptionTranslator(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
	                     AuthenticationException authException) throws IOException {
		resolve(request, response, authException, HttpStatus.UNAUTHORIZED);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
	                   AccessDeniedException accessDeniedException) throws IOException {
		resolve(request, response, accessDeniedException, HttpStatus.FORBIDDEN);
	}

	private void resolve(HttpServletRequest request, HttpServletResponse response, Exception e,
	                     HttpStatus fallbackStatus) throws IOException {
		if (resolver.resolveException(request, response, null, e) == null) {
			log.warn("보안 예외를 Problem Details로 변환하지 못해 기본 응답으로 대체합니다: status={}", fallbackStatus.value());
			response.sendError(fallbackStatus.value());
		}
	}
}
