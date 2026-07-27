package com.daesabu.meongcoach.shared.webapi;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginUser}가 붙은 Long 파라미터에 X-User-Id 헤더 값을 바인딩한다. Spring Security 도입 전까지 사용하는 임시 방식이며, 교체 시 이 클래스만 바꾸면 컨트롤러는
 * 그대로 둘 수 있다.
 */
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

	private static final String USER_ID_HEADER = "X-User-Id";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(LoginUser.class) && Long.class.equals(parameter.getParameterType());
	}

	// 헤더 누락은 MissingRequestHeaderException, 숫자 변환 실패는 TypeMismatchException으로 전파해
	// 에러 응답 변환을 전역 핸들러에 맡긴다 (exception-convention.md)
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
	                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		String headerValue = webRequest.getHeader(USER_ID_HEADER);
		if (headerValue == null) {
			throw new MissingRequestHeaderException(USER_ID_HEADER, parameter);
		}
		WebDataBinder binder = binderFactory.createBinder(webRequest, null, USER_ID_HEADER);
		return binder.convertIfNecessary(headerValue, Long.class, parameter);
	}
}
