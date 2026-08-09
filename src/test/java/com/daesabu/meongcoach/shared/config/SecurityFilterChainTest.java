package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SecurityConfig는 커버리지 검증에서 제외되므로, 실제 요청으로 필터 체인 동작을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("시큐리티 필터 체인")
class SecurityFilterChainTest {

	// 매핑되지 않은 보호 경로. 인증 없으면 401, 인증되면 401이 아닌 응답이 나온다
	private static final String PROTECTED_PATH = "/api/dogs";

	// @CurrentUserId를 받는 보호 경로. 없는 토픽을 골라 두어 토픽 조회 단계에서 404로 끝난다
	private static final String CURRENT_USER_PATH = "/api/training/topics/{topicId}";

	// 저장된 회원 ID에 더해 존재하지 않는 회원 ID를 만든다
	private static final long UNREGISTERED_ID_OFFSET = 1_000_000L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	// 액세스 토큰 검증이 회원 존재를 확인하므로 실제 회원 행이 있어야 인증을 통과한다
	private Long userId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(User.registerMember()).getId();
	}

	@Test
	@DisplayName("헬스 체크는 인증 없이 호출할 수 있다")
	void healthIsPermitted() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("토큰 재발급은 인증 없이 호출할 수 있다")
	void tokenRefreshIsPermitted() throws Exception {
		mockMvc.perform(post("/api/users/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("잘못된 소셜 토큰을 제출하면 401을 반환한다")
	void authenticatedSocialLoginWithInvalidCredentialReturnsUnauthorized() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(post("/api/users/social/kakao")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\": \"invalid\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"));
	}

	@Test
	@DisplayName("인증 엔드포인트가 아닌 회원 경로는 인증이 필요하다")
	void otherUserPathsAreProtected() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("토큰 없이 보호된 경로에 접근하면 Problem Details로 401을 반환한다")
	void protectedPathWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	@DisplayName("local 프로파일이 아니면 h2-console 경로도 인증이 필요하다")
	void h2ConsoleIsProtectedOutsideLocal() throws Exception {
		mockMvc.perform(get("/h2-console"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("위조된 토큰은 거부된다")
	void forgedTokenIsRejected() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("리프레시 토큰은 액세스 토큰 자리에서 거부된다")
	void refreshTokenIsRejectedAsBearerToken() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.refreshToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("유효한 액세스 토큰이면 인증을 통과한다")
	void validAccessTokenPassesAuthentication() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNotFound());
	}

	// 서명이 유효해도 회원 행이 없으면 통과시키지 않는다. DB가 초기화된 뒤 남아 있는 토큰이 이 경우다
	@Test
	@DisplayName("등록되지 않은 회원의 액세스 토큰은 거부된다")
	void accessTokenOfUnregisteredUserIsRejected() throws Exception {
		AuthToken token = tokenProvider.issue(userId + UNREGISTERED_ID_OFFSET);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로, 필터 체인이 세운 인증 주체를 CurrentUserIdArgumentResolver가
	// 실제로 읽어내는지는 여기에서만 확인할 수 있다. 401이 아니라 404면 해석에 성공한 것이다
	@Test
	@DisplayName("필터 체인이 세운 인증 주체가 @CurrentUserId 파라미터로 해석된다")
	void authenticatedRequestResolvesCurrentUserId() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(put(CURRENT_USER_PATH, 999L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"));
	}

	@Test
	@DisplayName("토큰 없이 @CurrentUserId 경로에 접근하면 401을 반환한다")
	void currentUserIdPathWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(put(CURRENT_USER_PATH, 999L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// test 프로파일은 meongcoach.api-docs.enabled를 두지 않아 기본값(false) 경로가 검증된다.
	// denyAll은 인증 여부와 무관하게 AccessDeniedException으로 끝나므로 미인증도 401이 아닌 403이다
	@Test
	@DisplayName("문서 비활성 환경에서는 토큰 없이 Swagger UI에 접근하면 403을 반환한다")
	void apiDocsDisabledWithoutTokenReturnsForbidden() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isForbidden());
	}

	// denyAll이 authenticated보다 우선함을 증명한다. 유효 토큰 소지자도 문서를 볼 수 없어야 한다
	@Test
	@DisplayName("문서 비활성 환경에서는 유효한 토큰으로도 Swagger UI에 접근할 수 없다")
	void apiDocsDisabledWithValidTokenReturnsForbidden() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get("/swagger-ui/index.html")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden());
	}
}
