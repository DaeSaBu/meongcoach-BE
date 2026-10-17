package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class AdminKeyInterceptorTest {

	private static final String ADMIN_KEY = "test-admin-key-with-enough-length";

	private final AdminKeyInterceptor interceptor = new AdminKeyInterceptor(ADMIN_KEY);

	@Test
	void 관리자_키가_일치하면_요청을_통과시킨다() throws Exception {
		MockHttpServletRequest request = adminRequest();
		request.addHeader(AdminKeyInterceptor.HEADER, ADMIN_KEY);

		boolean passed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

		assertThat(passed).isTrue();
	}

	@Test
	void 관리자_키_헤더가_없으면_인증_예외를_던진다() {
		MockHttpServletRequest request = adminRequest();

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void 관리자_키가_다르면_인증_예외를_던진다() {
		MockHttpServletRequest request = adminRequest();
		request.addHeader(AdminKeyInterceptor.HEADER, "wrong-key");

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void 관리자_키가_설정되지_않았으면_경로가_없는_것처럼_404_예외를_던진다() {
		AdminKeyInterceptor disabled = new AdminKeyInterceptor("");
		MockHttpServletRequest request = adminRequest();
		request.addHeader(AdminKeyInterceptor.HEADER, "");

		assertThatThrownBy(() -> disabled.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(NoResourceFoundException.class);
	}

	private MockHttpServletRequest adminRequest() {
		return new MockHttpServletRequest("GET", "/api/admin/graduations/submissions");
	}
}
