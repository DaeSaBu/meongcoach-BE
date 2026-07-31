package com.daesabu.meongcoach.shared.security;

import java.security.Principal;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUserId} 파라미터에 인증 주체의 이름(JWT sub = 사용자 ID)을 Long으로 변환해 주입한다.
 * 인증 정보가 없거나 형식이 잘못되면 인증 예외를 던져 전역 핸들러가 401로 변환하게 한다.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserId.class)
				&& Long.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
	                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Principal principal = webRequest.getUserPrincipal();
		if (principal == null) {
			throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
		}
		try {
			return Long.valueOf(principal.getName());
		} catch (NumberFormatException e) {
			// 예외 detail은 응답에 노출되므로 주체 값을 담지 않는다
			throw new AuthenticationCredentialsNotFoundException("인증 정보가 올바르지 않습니다.");
		}
	}
}
