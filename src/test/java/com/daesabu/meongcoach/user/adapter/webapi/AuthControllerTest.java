package com.daesabu.meongcoach.user.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.LocalLogin;
import com.daesabu.meongcoach.user.application.provided.LoginResult;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
import com.daesabu.meongcoach.user.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.StubConfig.class)
@AutoConfigureRestDocs
@DisplayName("인증 API")
class AuthControllerTest {

	private static final String VALID_TOKEN = "kakao-id-token";
	private static final String VALID_REFRESH_TOKEN = "valid-refresh-token";
	private static final String VALID_EMAIL = "review@meongcoach.com";
	private static final String VALID_PASSWORD = "meongcoach-review";

	@Autowired
	private MockMvc mockMvc;

	private static String loginBody(String token) {
		return "{\"token\": \"" + token + "\"}";
	}

	private static String refreshBody(String refreshToken) {
		return "{\"refreshToken\": \"" + refreshToken + "\"}";
	}

	private static String localLoginBody(String email, String password) {
		return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
	}

	@Test
	@DisplayName("소셜 로그인하면 토큰과 온보딩 필요 여부를 반환한다")
	void loginReturnsTokens() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(VALID_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andDo(document("auth/login",
						pathParameters(
								parameterWithName("provider").description("소셜 로그인 제공자. `kakao`, `apple` 지원")
						),
						requestFields(
								fieldWithPath("token").description(
										"필수 입력. 앱이 제공자 SDK로 받은 OIDC ID 토큰(애플은 identityToken)")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰"),
								fieldWithPath("needsOnboarding").description("온보딩 화면으로 보내야 하는지 여부")
						)
				));
	}

	@Test
	@DisplayName("제공자가 토큰을 거부하면 401을 반환한다")
	void loginFailsWhenSocialTokenIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"))
				.andDo(document("auth/login-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	@DisplayName("지원하지 않는 제공자면 400을 반환한다")
	void loginFailsWhenProviderIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/{provider}", "naver")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(VALID_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_UNSUPPORTED_SOCIAL_PROVIDER"));
	}

	@Test
	@DisplayName("자격증명이 비어 있으면 검증에 실패한다")
	void loginFailsWhenSocialTokenIsBlank() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("token"));
	}

	@Test
	void 이메일과_비밀번호로_로그인하면_토큰과_온보딩_필요_여부를_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(localLoginBody(VALID_EMAIL, VALID_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andDo(document("auth/local-login",
						requestFields(
								fieldWithPath("email").description("필수 입력. 서버에 등록된 테스트 계정 이메일"),
								fieldWithPath("password").description("필수 입력. 테스트 계정 비밀번호")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰"),
								fieldWithPath("needsOnboarding").description("온보딩 화면으로 보내야 하는지 여부")
						)
				));
	}

	// 이메일 미존재와 비밀번호 불일치를 구분하면 계정 존재 여부가 드러나므로 같은 응답이어야 한다
	@Test
	void 이메일_또는_비밀번호가_틀리면_원인을_구분하지_않고_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(localLoginBody(VALID_EMAIL, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"))
				.andDo(document("auth/local-login-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 이메일이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(localLoginBody("", VALID_PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	@DisplayName("리프레시 토큰으로 새 토큰을 발급받는다")
	void refreshReturnsNewTokens() throws Exception {
		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody(VALID_REFRESH_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").doesNotExist())
				.andDo(document("auth/token-refresh",
						requestFields(
								fieldWithPath("refreshToken").description(
										"필수 입력. 로그인 시 발급받은 리프레시 토큰")
						),
						responseFields(
								fieldWithPath("accessToken").description("새로 발급된 액세스 토큰"),
								fieldWithPath("refreshToken").description("새로 발급된 리프레시 토큰")
						)
				));
	}

	@Test
	@DisplayName("유효하지 않은 리프레시 토큰이면 401을 반환한다")
	void refreshFailsWhenRefreshTokenIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_REFRESH_TOKEN"))
				.andDo(document("auth/token-refresh-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	@DisplayName("리프레시 토큰이 비어 있으면 검증에 실패한다")
	void refreshFailsWhenRefreshTokenIsBlank() throws Exception {
		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		SocialLogin socialLogin() {
			return (provider, credential) -> {
				if (!VALID_TOKEN.equals(credential)) {
					throw new InvalidSocialTokenException();
				}
				return new LoginResult(new AuthToken("access-token", "refresh-token"), true);
			};
		}

		@Bean
		LocalLogin localLogin() {
			return (email, password) -> {
				if (!VALID_EMAIL.equals(email) || !VALID_PASSWORD.equals(password)) {
					throw new InvalidCredentialsException();
				}
				return new LoginResult(new AuthToken("access-token", "refresh-token"), true);
			};
		}

		@Bean
		TokenRefresher tokenRefresher() {
			return refreshToken -> {
				if (!VALID_REFRESH_TOKEN.equals(refreshToken)) {
					throw new InvalidRefreshTokenException();
				}
				return new AuthToken("access-token", "refresh-token");
			};
		}
	}
}
