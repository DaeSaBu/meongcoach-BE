package com.daesabu.meongcoach.auth.adapter.webapi.legacy;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.provided.Authenticator;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.exception.AppleAuthorizationCodeRequiredException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidEmailException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.auth.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.security.Principal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 구 클라이언트가 호출하는 인증 경로·본문·에러 코드가 신 구현 위에서 그대로 유지되는지 검증한다.
 * 구 앱 지원이 끝나면 LegacyAuthController와 함께 삭제한다.
 */
@WebMvcTest(LegacyAuthController.class)
@AutoConfigureRestDocs
class LegacyAuthControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다
	private static final Principal CURRENT_USER = () -> "42";
	private static final Long USER_ID = 42L;
	private static final String ID_TOKEN = "kakao-id-token";
	private static final String EMAIL = "review@meongcoach.com";
	private static final String PASSWORD = "meongcoach-review";
	private static final String APPLE_CODE = "c1a2b3.0.abcd.efgh";
	private static final AuthToken AUTH_TOKEN = new AuthToken(USER_ID, "access-token", "refresh-token",
			new RefreshTokenId("0f8fad5b-d9cb-469f-a165-70867728950e"), LocalDateTime.of(2026, 9, 16, 12, 0));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

	@MockitoBean
	private UserFinder userFinder;

	private static String socialLoginBody(String token) {
		return "{\"token\": \"" + token + "\"}";
	}

	private static String emailLoginBody(String email, String password) {
		return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
	}

	private static String withdrawBody(String appleAuthorizationCode) {
		return "{\"appleAuthorizationCode\": \"" + appleAuthorizationCode + "\"}";
	}

	@Test
	void 구_경로로_소셜_로그인하면_온보딩_여부를_포함한_토큰을_반환한다() throws Exception {
		given(authenticator.socialLogin(new SocialLoginRequest("kakao", ID_TOKEN))).willReturn(AUTH_TOKEN);
		given(userFinder.isOnboardingUser(USER_ID)).willReturn(true);

		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody(ID_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").value(true))
				.andDo(document("auth/legacy-social-login",
						pathParameters(
								parameterWithName("provider").description("소셜 로그인 제공자. `kakao`, `google`, `apple`(대소문자 무시)")
						),
						requestFields(
								fieldWithPath("token").description("필수 입력. 앱이 제공자 SDK로 받은 OIDC ID 토큰")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰"),
								fieldWithPath("needsOnboarding").description("온보딩 화면으로 보내야 하는지 여부")
						)
				));
	}

	@Test
	void 정회원이_구_경로로_소셜_로그인하면_온보딩이_필요하지_않다고_응답한다() throws Exception {
		given(authenticator.socialLogin(new SocialLoginRequest("KAKAO", ID_TOKEN))).willReturn(AUTH_TOKEN);
		given(userFinder.isOnboardingUser(USER_ID)).willReturn(false);

		mockMvc.perform(post("/api/auth/login/social/{provider}", "KAKAO")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody(ID_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.needsOnboarding").value(false));
	}

	// 구 앱은 USER_ 접두어 코드로 분기하므로 레거시 경로에서는 AUTH_ 코드를 USER_로 되돌려 내린다
	@Test
	void 구_경로에서_제공자가_토큰을_거부하면_USER_접두어_에러_코드로_401을_반환한다() throws Exception {
		given(authenticator.socialLogin(any())).willThrow(new InvalidSocialTokenException());

		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"))
				.andExpect(jsonPath("$.detail").value("소셜 로그인 토큰이 유효하지 않습니다."))
				.andDo(document("auth/legacy-social-login-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드(구 클라이언트용 `USER_` 접두어)"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 구_경로에서_지원하지_않는_제공자면_USER_접두어_에러_코드로_400을_반환한다() throws Exception {
		given(authenticator.socialLogin(any())).willThrow(new UnsupportedSocialProviderException("naver"));

		mockMvc.perform(post("/api/auth/login/social/{provider}", "naver")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody(ID_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_UNSUPPORTED_SOCIAL_PROVIDER"));
	}

	@Test
	void 구_경로에서_토큰이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/{provider}", "kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("token"));
	}

	@Test
	void 구_경로로_이메일_로그인하면_온보딩_여부를_포함한_토큰을_반환한다() throws Exception {
		given(authenticator.emailLogin(new EmailLoginRequest(EMAIL, PASSWORD))).willReturn(AUTH_TOKEN);
		given(userFinder.isOnboardingUser(USER_ID)).willReturn(false);

		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody(EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").value(false))
				.andDo(document("auth/legacy-email-login",
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

	@Test
	void 구_경로에서_자격증명이_틀리면_USER_접두어_에러_코드로_401을_반환한다() throws Exception {
		given(authenticator.emailLogin(any())).willThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody(EMAIL, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"))
				.andDo(document("auth/legacy-email-login-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드(구 클라이언트용 `USER_` 접두어)"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 구_경로에서_이메일_형식이_올바르지_않으면_USER_접두어_에러_코드로_400을_반환한다() throws Exception {
		given(authenticator.emailLogin(any())).willThrow(new InvalidEmailException());

		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody("not-an-email", PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_INVALID_EMAIL"));
	}

	@Test
	void 구_경로로_탈퇴하면_인증_주체의_회원_ID와_인가_코드로_위임하고_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(withdrawBody(APPLE_CODE)))
				.andExpect(status().isNoContent())
				.andDo(document("auth/legacy-withdraw",
						requestFields(
								fieldWithPath("appleAuthorizationCode").optional()
										.description("탈퇴 직전 Sign in with Apple 재인증으로 받은 authorizationCode. "
												+ "Apple 계정 회원은 필수(5분 만료·1회용), 그 외 회원은 생략")
						)
				));

		then(authenticator).should().withdraw(USER_ID, new WithdrawRequest(APPLE_CODE));
	}

	@Test
	void 구_경로로_본문_없이_탈퇴해도_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/users/me").principal(CURRENT_USER))
				.andExpect(status().isNoContent());

		then(authenticator).should().withdraw(USER_ID, null);
	}

	// 구 앱은 이 코드로 Apple 재인증 후 재요청을 트리거하므로 접두어가 바뀌면 Apple 계정 탈퇴가 막힌다
	@Test
	void 구_경로에서_Apple_회원이_인가_코드_없이_탈퇴하면_USER_접두어_에러_코드로_400을_반환한다() throws Exception {
		willThrow(new AppleAuthorizationCodeRequiredException()).given(authenticator).withdraw(USER_ID, null);

		mockMvc.perform(delete("/api/users/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_APPLE_AUTHORIZATION_CODE_REQUIRED"))
				.andDo(document("auth/legacy-withdraw-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드(구 클라이언트용 `USER_` 접두어)"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	// auth 모듈 코드만 되돌리고 다른 모듈의 코드는 원래부터 같은 이름이므로 손대지 않는다
	@Test
	void 구_경로에서_auth_모듈이_아닌_에러_코드는_바꾸지_않는다() throws Exception {
		willThrow(new UserNotFoundException(USER_ID)).given(authenticator).withdraw(USER_ID, null);

		mockMvc.perform(delete("/api/users/me").principal(CURRENT_USER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void 구_경로로_인증_정보_없이_탈퇴하면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/api/users/me"))
				.andExpect(status().isUnauthorized());

		then(authenticator).shouldHaveNoInteractions();
	}
}
