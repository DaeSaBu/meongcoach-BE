package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@DisplayName("시큐리티 예외 위임")
class SecurityExceptionTranslatorTest {

	@Test
	@DisplayName("인증 예외를 MVC 예외 처리기로 넘긴다")
	void commenceDelegatesAuthenticationException() throws Exception {
		RecordingResolver resolver = new RecordingResolver(new ModelAndView());
		BadCredentialsException exception = new BadCredentialsException("자격증명 오류");

		new SecurityExceptionTranslator(resolver).commence(new MockHttpServletRequest(),
				new MockHttpServletResponse(), exception);

		assertThat(resolver.handled).isSameAs(exception);
		assertThat(resolver.handler).isNull();
	}

	@Test
	@DisplayName("인가 예외를 MVC 예외 처리기로 넘긴다")
	void handleDelegatesAccessDeniedException() throws Exception {
		RecordingResolver resolver = new RecordingResolver(new ModelAndView());
		AccessDeniedException exception = new AccessDeniedException("권한 없음");

		new SecurityExceptionTranslator(resolver).handle(new MockHttpServletRequest(),
				new MockHttpServletResponse(), exception);

		assertThat(resolver.handled).isSameAs(exception);
	}

	@Test
	@DisplayName("예외 처리기가 응답을 만들지 못하면 기본 상태 코드로 대체한다")
	void commenceFallsBackWhenResolverReturnsNull() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		new SecurityExceptionTranslator(new RecordingResolver(null)).commence(new MockHttpServletRequest(),
				response, new BadCredentialsException("자격증명 오류"));

		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	@DisplayName("인가 예외도 처리기가 실패하면 기본 상태 코드로 대체한다")
	void handleFallsBackWhenResolverReturnsNull() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		new SecurityExceptionTranslator(new RecordingResolver(null)).handle(new MockHttpServletRequest(),
				response, new AccessDeniedException("권한 없음"));

		assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
	}

	private static class RecordingResolver implements HandlerExceptionResolver {

		private final ModelAndView result;

		private Exception handled;
		private Object handler;

		RecordingResolver(ModelAndView result) {
			this.result = result;
		}

		@Override
		public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
		                                     Object handler, Exception ex) {
			this.handled = ex;
			this.handler = handler;
			return result;
		}
	}
}
