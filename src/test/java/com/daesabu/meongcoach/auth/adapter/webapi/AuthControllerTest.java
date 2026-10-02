package com.daesabu.meongcoach.auth.adapter.webapi;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.provided.Authenticator;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.LogoutRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.auth.domain.RefreshTokenId;
import com.daesabu.meongcoach.auth.domain.exception.AppleAuthorizationCodeRequiredException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidCredentialsException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidEmailException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.auth.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.auth.domain.exception.UnsupportedSocialProviderException;
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

@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다
	private static final Principal CURRENT_USER = () -> "42";
	private static final String ID_TOKEN = "kakao-id-token";
	private static final String REFRESH_TOKEN = "valid-refresh-token";
	private static final String EMAIL = "review@meongcoach.com";
	private static final String PASSWORD = "meongcoach-review";
	private static final String APPLE_CODE = "c1a2b3.0.abcd.efgh";
	private static final AuthToken AUTH_TOKEN = new AuthToken(42L, "access-token", "refresh-token",
			new RefreshTokenId("0f8fad5b-d9cb-469f-a165-70867728950e"), LocalDateTime.of(2026, 9, 16, 12, 0));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

	private static String socialLoginBody(String provider, String idToken) {
		return "{\"socialProvider\": \"" + provider + "\", \"idToken\": \"" + idToken + "\"}";
	}

	private static String emailLoginBody(String email, String password) {
		return "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
	}

	private static String refreshBody(String refreshToken) {
		return "{\"refreshToken\": \"" + refreshToken + "\"}";
	}

	private static String withdrawBody(String appleAuthorizationCode) {
		return "{\"appleAuthorizationCode\": \"" + appleAuthorizationCode + "\"}";
	}

	@Test
	void 소셜_로그인하면_토큰을_반환한다() throws Exception {
		given(authenticator.socialLogin(new SocialLoginRequest("kakao", ID_TOKEN))).willReturn(AUTH_TOKEN);

		mockMvc.perform(post("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("kakao", ID_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.needsOnboarding").doesNotExist())
				.andDo(document("auth/login",
						requestFields(
								fieldWithPath("socialProvider").description(
										"필수 입력. 소셜 로그인 제공자. `kakao`, `google`, `apple` 지원(대소문자 무시)"),
								fieldWithPath("idToken").description(
										"필수 입력. 앱이 제공자 SDK로 받은 OIDC ID 토큰(애플은 identityToken)")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰")
						)
				));
	}

	@Test
	void 제공자가_토큰을_거부하면_401을_반환한다() throws Exception {
		given(authenticator.socialLogin(any())).willThrow(new InvalidSocialTokenException());

		mockMvc.perform(post("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("KAKAO", "invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_SOCIAL_TOKEN"))
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
	void 지원하지_않는_제공자면_400을_반환한다() throws Exception {
		given(authenticator.socialLogin(any())).willThrow(new UnsupportedSocialProviderException("naver"));

		mockMvc.perform(post("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("naver", ID_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_UNSUPPORTED_SOCIAL_PROVIDER"));
	}

	@Test
	void 제공자가_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("", ID_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("socialProvider"));
	}

	@Test
	void ID_토큰이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content(socialLoginBody("KAKAO", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("idToken"));
	}

	@Test
	void 이메일과_비밀번호로_로그인하면_토큰을_반환한다() throws Exception {
		given(authenticator.emailLogin(new EmailLoginRequest(EMAIL, PASSWORD))).willReturn(AUTH_TOKEN);

		mockMvc.perform(post("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody(EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andDo(document("auth/email-login",
						requestFields(
								fieldWithPath("email").description("필수 입력. 서버에 등록된 테스트 계정 이메일"),
								fieldWithPath("password").description("필수 입력. 테스트 계정 비밀번호")
						),
						responseFields(
								fieldWithPath("accessToken").description("API 호출에 사용할 액세스 토큰"),
								fieldWithPath("refreshToken").description("액세스 토큰 재발급용 리프레시 토큰")
						)
				));
	}

	// 이메일 미존재와 비밀번호 불일치를 구분하면 계정 존재 여부가 드러나므로 같은 응답이어야 한다
	@Test
	void 이메일_또는_비밀번호가_틀리면_원인을_구분하지_않고_401을_반환한다() throws Exception {
		given(authenticator.emailLogin(any())).willThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody(EMAIL, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
				.andDo(document("auth/email-login-error",
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
	void 이메일_형식이_올바르지_않으면_400을_반환한다() throws Exception {
		given(authenticator.emailLogin(any())).willThrow(new InvalidEmailException());

		mockMvc.perform(post("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody("not-an-email", PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_EMAIL"));
	}

	@Test
	void 이메일이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content(emailLoginBody("", PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	void 리프레시_토큰으로_새_토큰을_발급받는다() throws Exception {
		given(authenticator.refresh(new TokenRefreshRequest(REFRESH_TOKEN))).willReturn(AUTH_TOKEN);

		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody(REFRESH_TOKEN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
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
	void 유효하지_않은_리프레시_토큰이면_401을_반환한다() throws Exception {
		given(authenticator.refresh(any())).willThrow(new InvalidRefreshTokenException());

		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"))
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
	void 리프레시_토큰이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
	}

	@Test
	void 로그아웃하면_204를_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody(REFRESH_TOKEN)))
				.andExpect(status().isNoContent())
				.andDo(document("auth/logout",
						requestFields(
								fieldWithPath("refreshToken").description("필수 입력. 폐기할 리프레시 토큰")
						)
				));

		then(authenticator).should().logout(new LogoutRequest(REFRESH_TOKEN));
	}

	@Test
	void 유효하지_않은_리프레시_토큰으로_로그아웃하면_401을_반환한다() throws Exception {
		willThrow(new InvalidRefreshTokenException()).given(authenticator).logout(any());

		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("invalid")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"))
				.andDo(document("auth/logout-error",
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
	void 로그아웃_리프레시_토큰이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("refreshToken"));
	}

	@Test
	void 탈퇴하면_인증_주체의_회원_ID와_인가_코드로_위임하고_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/auth/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(withdrawBody(APPLE_CODE)))
				.andExpect(status().isNoContent())
				.andDo(document("auth/withdraw",
						requestFields(
								fieldWithPath("appleAuthorizationCode").optional()
										.description("탈퇴 직전 Sign in with Apple 재인증으로 받은 authorizationCode. "
												+ "Apple 계정 회원은 필수(5분 만료·1회용), 그 외 회원은 생략")
						)
				));

		then(authenticator).should().withdraw(42L, new WithdrawRequest(APPLE_CODE));
	}

	// Apple 계정 회원만 본문을 보내므로 본문은 선택이다
	@Test
	void 본문_없이_탈퇴해도_204를_반환한다() throws Exception {
		mockMvc.perform(delete("/api/auth/me").principal(CURRENT_USER))
				.andExpect(status().isNoContent());

		then(authenticator).should().withdraw(42L, null);
	}

	@Test
	void Apple_회원이_인가_코드_없이_탈퇴하면_400과_에러_코드를_반환한다() throws Exception {
		willThrow(new AppleAuthorizationCodeRequiredException()).given(authenticator).withdraw(42L, null);

		mockMvc.perform(delete("/api/auth/me").principal(CURRENT_USER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_APPLE_AUTHORIZATION_CODE_REQUIRED"));
	}

	@Test
	void 없는_회원이_탈퇴하면_404와_에러_코드를_반환한다() throws Exception {
		willThrow(new UserNotFoundException(42L)).given(authenticator).withdraw(42L, null);

		mockMvc.perform(delete("/api/auth/me")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 42인 회원을 찾을 수 없습니다."))
				.andDo(document("auth/withdraw-error",
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
	void 인증_정보_없이_탈퇴하면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/api/auth/me"))
				.andExpect(status().isUnauthorized());

		then(authenticator).shouldHaveNoInteractions();
	}
}
