package com.daesabu.meongcoach.shared.webapi;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginUser}가 붙은 Long 파라미터에 액세스 토큰의 sub 클레임(회원 ID)을 바인딩한다. 토큰 검증은 시큐리티 필터 체인이 이미 끝냈으므로 여기서는 인증 주체를
 * 회원 ID로 해석하기만 한다. 컨트롤러가 인증 방식에 묶이지 않도록 이 해석은 이 클래스 한 곳에서만 한다.
 */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(LoginUser.class) && Long.class.equals(parameter.getParameterType());
	}

	// 인증 예외는 AuthenticationException 계열로 던져 전역 핸들러가 401 Problem Details로 변환하게 한다
	// (exception-convention.md)
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
	                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
		}
		try {
			return Long.valueOf(jwt.getSubject());
		} catch (NumberFormatException e) {
			// 예외 메시지는 로그에 남으므로 토큰 값이나 sub 값을 담지 않는다
			throw new InvalidBearerTokenException("액세스 토큰의 sub 클레임이 회원 ID가 아닙니다.");
		}
	}
}
