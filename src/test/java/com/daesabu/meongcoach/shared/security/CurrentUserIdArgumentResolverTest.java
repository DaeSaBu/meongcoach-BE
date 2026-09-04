package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.NativeWebRequest;

class CurrentUserIdArgumentResolverTest {

	private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

	@SuppressWarnings("unused")
	private static class TestController {

		void annotated(@CurrentUserId Long userId) {
		}

		void notAnnotated(Long userId) {
		}

		void wrongType(@CurrentUserId String userId) {
		}
	}

	private MethodParameter parameterOf(String methodName, Class<?> parameterType) throws NoSuchMethodException {
		return new MethodParameter(TestController.class.getDeclaredMethod(methodName, parameterType), 0);
	}

	@Test
	void CurrentUserId가_붙은_Long_파라미터를_지원한다() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("annotated", Long.class))).isTrue();
	}

	@Test
	void 애노테이션이_없는_파라미터는_지원하지_않는다() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("notAnnotated", Long.class))).isFalse();
	}

	@Test
	void Long이_아닌_타입의_파라미터는_지원하지_않는다() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
	}

	@Test
	void 인증_주체의_이름을_Long_사용자_ID로_변환한다() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(() -> "1");

		Object resolved = resolver.resolveArgument(null, null, webRequest, null);

		assertThat(resolved).isEqualTo(1L);
	}

	@Test
	void 인증_주체가_없으면_인증_예외를_던진다() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(null);

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	void 인증_주체의_이름이_숫자가_아니면_인증_예외를_던진다() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(() -> "not-a-number");

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}
}
