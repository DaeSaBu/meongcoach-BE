package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class SecurityExceptionTranslatorTest {

	@Test
	void 인증_예외를_MVC_예외_처리기로_넘긴다() throws Exception {
		RecordingResolver resolver = new RecordingResolver(new ModelAndView());
		BadCredentialsException exception = new BadCredentialsException("자격증명 오류");

		new SecurityExceptionTranslator(resolver).commence(new MockHttpServletRequest(),
				new MockHttpServletResponse(), exception);

		assertThat(resolver.handled).isSameAs(exception);
		assertThat(resolver.handler).isNull();
	}

	@Test
	void 인가_예외를_MVC_예외_처리기로_넘긴다() throws Exception {
		RecordingResolver resolver = new RecordingResolver(new ModelAndView());
		AccessDeniedException exception = new AccessDeniedException("권한 없음");

		new SecurityExceptionTranslator(resolver).handle(new MockHttpServletRequest(),
				new MockHttpServletResponse(), exception);

		assertThat(resolver.handled).isSameAs(exception);
	}

	@Test
	void 예외_처리기가_응답을_만들지_못하면_기본_상태_코드로_대체한다() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		new SecurityExceptionTranslator(new RecordingResolver(null)).commence(new MockHttpServletRequest(),
				response, new BadCredentialsException("자격증명 오류"));

		assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void 인가_예외도_처리기가_실패하면_기본_상태_코드로_대체한다() throws Exception {
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
