package com.daesabu.meongcoach.user.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.provided.TokenRefresher;
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
@Import({AuthControllerTest.StubConfig.class, SocialTokenIssueHandler.class, RefreshTokenIssueHandler.class})
@AutoConfigureRestDocs
@DisplayName("인증 API")
class AuthControllerTest {

	private static final String VALID_TOKEN = "kakao-id-token";
	private static final String VALID_REFRESH_TOKEN = "valid-refresh-token";

	@Autowired
	private MockMvc mockMvc;

	private static String socialGrantBody(String provider, String token) {
		return "{\"grantType\": \"social\", \"provider\": \"" + provider + "\", \"token\": \"" + token + "\"}";
	}

	private static String refreshGrantBody(String refreshToken) {
		return "{\"grantType\": \"refresh\", \"refreshToken\": \"" + refreshToken + "\"}";
	}

	@Test
	@DisplayName("social 발급 방식으로 로그인하면 토큰과 온보딩 필요 여부를 반환한다")
	void issueTokenWithSocialGrantReturnsTokens() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialGrantBody("kakao", VALID_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andDo(document("auth/token-issue-social",
						requestFields(
								fieldWithPath("grantType").description(
										"필수 입력. 발급 방식. `social`(소셜 로그인) 또는 `refresh`(토큰 갱신)"),
								fieldWithPath("provider").description(
										"social 발급 시 필수 입력. 소셜 로그인 제공자. 현재 `kakao`만 지원"),
								fieldWithPath("token").description(
										"social 발급 시 필수 입력. 앱이 제공자 SDK로 받은 자격증명. 카카오는 OIDC ID 토큰")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰"),
								fieldWithPath("needsOnboarding").description(
										"온보딩 화면으로 보내야 하는지 여부. social 발급에서만 내려간다")
						)
				));
	}

	@Test
	@DisplayName("제공자가 토큰을 거부하면 401을 반환한다")
	void issueTokenFailsWhenSocialTokenIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialGrantBody("kakao", "invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"))
				.andDo(document("auth/token-issue-social-error",
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
	void issueTokenFailsWhenProviderIsUnsupported() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialGrantBody("naver", VALID_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_UNSUPPORTED_SOCIAL_PROVIDER"));
	}

	@Test
	@DisplayName("social 발급에서 자격증명이 비어 있으면 검증에 실패한다")
	void issueTokenFailsWhenSocialTokenIsBlank() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialGrantBody("kakao", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("tokenPresent"));
	}

	@Test
	@DisplayName("grantType이 없거나 알 수 없는 값이면 검증에 실패한다")
	void issueTokenFailsWhenGrantTypeIsUnknown() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"grantType\": \"password\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("grantType"));
	}

	@Test
	@DisplayName("refresh 발급 방식으로 새 토큰을 발급받는다")
	void issueTokenWithRefreshGrantReturnsNewTokens() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshGrantBody(VALID_REFRESH_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").doesNotExist())
				.andDo(document("auth/token-issue-refresh",
						requestFields(
								fieldWithPath("grantType").description(
										"필수 입력. 발급 방식. `social`(소셜 로그인) 또는 `refresh`(토큰 갱신)"),
								fieldWithPath("refreshToken").description(
										"refresh 발급 시 필수 입력. 로그인 시 발급받은 리프레시 토큰")
						),
						responseFields(
								fieldWithPath("accessToken").description("새로 발급된 액세스 토큰"),
								fieldWithPath("refreshToken").description("새로 발급된 리프레시 토큰")
						)
				));
	}

	@Test
	@DisplayName("유효하지 않은 리프레시 토큰이면 401을 반환한다")
	void issueTokenFailsWhenRefreshTokenIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshGrantBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_REFRESH_TOKEN"))
				.andDo(document("auth/token-issue-refresh-error",
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
	@DisplayName("refresh 발급에서 리프레시 토큰이 비어 있으면 검증에 실패한다")
	void issueTokenFailsWhenRefreshTokenIsBlank() throws Exception {
		mockMvc.perform(post("/api/auth/tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshGrantBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("refreshTokenPresent"));
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		SocialLogin socialLogin() {
			return (provider, credential) -> {
				if (!VALID_TOKEN.equals(credential)) {
					throw new InvalidSocialTokenException();
				}
				return new SocialLoginResult(new AuthToken("access-token", "refresh-token"), true);
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
