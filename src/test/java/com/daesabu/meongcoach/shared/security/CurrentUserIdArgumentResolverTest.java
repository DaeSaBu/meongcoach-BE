package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.NativeWebRequest;

@DisplayName("CurrentUserIdArgumentResolver")
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
	@DisplayName("@CurrentUserId가 붙은 Long 파라미터를 지원한다")
	void supportsAnnotatedLongParameter() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("annotated", Long.class))).isTrue();
	}

	@Test
	@DisplayName("애노테이션이 없는 파라미터는 지원하지 않는다")
	void doesNotSupportParameterWithoutAnnotation() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("notAnnotated", Long.class))).isFalse();
	}

	@Test
	@DisplayName("Long이 아닌 타입의 파라미터는 지원하지 않는다")
	void doesNotSupportNonLongParameter() throws NoSuchMethodException {
		assertThat(resolver.supportsParameter(parameterOf("wrongType", String.class))).isFalse();
	}

	@Test
	@DisplayName("인증 주체의 이름을 Long 사용자 ID로 변환한다")
	void resolvesPrincipalNameAsUserId() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(() -> "1");

		Object resolved = resolver.resolveArgument(null, null, webRequest, null);

		assertThat(resolved).isEqualTo(1L);
	}

	@Test
	@DisplayName("인증 주체가 없으면 인증 예외를 던진다")
	void throwsWhenPrincipalIsMissing() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(null);

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	@DisplayName("인증 주체의 이름이 숫자가 아니면 인증 예외를 던진다")
	void throwsWhenPrincipalNameIsNotNumeric() {
		NativeWebRequest webRequest = mock(NativeWebRequest.class);
		given(webRequest.getUserPrincipal()).willReturn(() -> "not-a-number");

		assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}
}
