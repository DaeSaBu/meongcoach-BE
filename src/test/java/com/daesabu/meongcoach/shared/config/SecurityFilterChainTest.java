package com.daesabu.meongcoach.shared.config;

import static com.daesabu.meongcoach.support.AppClientRequests.appDelete;
import static com.daesabu.meongcoach.support.AppClientRequests.appGet;
import static com.daesabu.meongcoach.support.AppClientRequests.appPost;
import static com.daesabu.meongcoach.support.AppClientRequests.appPut;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

	private static final String PROTECTED_PATH = "/api/dogs";

	private static final String CURRENT_USER_PATH = "/api/training/topic/selection";

	private static final String MISSING_TOPIC_SELECTION_BODY = "{\"topicId\": 999}";

	private static final long UNREGISTERED_ID_OFFSET = 1_000_000L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	private Long userId;

	private Long onboardingUserId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(promotedUser()).getId();
		onboardingUserId = userRepository.save(User.registerUser()).getId();
	}

	private User promotedUser() {
		User user = User.registerUser();
		user.promoteToUser();
		return user;
	}

	@Test
	void 헬스_체크는_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void 토큰_재발급은_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(appPost("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 로그아웃은_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(appPost("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 잘못된_소셜_토큰을_제출하면_401을_반환한다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(appPost("/api/auth/login/social")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"socialProvider\": \"KAKAO\", \"idToken\": \"invalid\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_SOCIAL_TOKEN"));
	}

	@Test
	void 애플_로그인_경로도_인증_없이_열려_있고_잘못된_토큰이면_401을_반환한다() throws Exception {
		mockMvc.perform(appPost("/api/auth/login/social")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"socialProvider\": \"apple\", \"idToken\": \"invalid\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_SOCIAL_TOKEN"));
	}

	@Test
	void 이메일_로그인_경로는_인증_없이_열려_있고_자격증명이_틀리면_401을_반환한다() throws Exception {
		mockMvc.perform(appPost("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"nobody@meongcoach.com\", \"password\": \"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void 회원_경로는_인증이_필요하다() throws Exception {
		mockMvc.perform(appGet("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 토큰_없이_보호된_경로에_접근하면_Problem_Details로_401을_반환한다() throws Exception {
		mockMvc.perform(appGet(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void 위조된_토큰은_거부된다() throws Exception {
		mockMvc.perform(appGet(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 리프레시_토큰은_액세스_토큰_자리에서_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(appGet(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.refreshToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 유효한_액세스_토큰이면_인증을_통과한다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(appGet(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	@Test
	void 등록되지_않은_회원의_액세스_토큰은_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId + UNREGISTERED_ID_OFFSET);

		mockMvc.perform(appGet(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 필터_체인이_세운_인증_주체가_CurrentUserId_파라미터로_해석된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(appPut(CURRENT_USER_PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MISSING_TOPIC_SELECTION_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"));
	}

	@Test
	void 토큰_없이_CurrentUserId_경로에_접근하면_401을_반환한다() throws Exception {
		mockMvc.perform(appPut(CURRENT_USER_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(MISSING_TOPIC_SELECTION_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 온보딩_미완료_회원이_정회원_전용_경로에_접근하면_ONBOARDING_NOT_COMPLETED_403을_반환한다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appGet("/api/training/topic/selection/curriculums")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"));
	}

	@Test
	void 온보딩_미완료_회원도_온보딩_메타데이터를_조회할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appGet("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	@Test
	void 온보딩_미완료_회원도_강아지_프로필_이미지_경로에_접근할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appGet("/api/dogs/profile/image")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void 온보딩_미완료_회원도_탈퇴할_수_있고_탈퇴_후_같은_토큰은_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appDelete("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNoContent());

		mockMvc.perform(appGet("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 온보딩_미완료_회원도_내_정보를_조회할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appGet("/api/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.needsOnboarding").value(true));
	}

	@Test
	void 온보딩_미완료_회원은_허용된_메서드_외에는_회원_경로에_접근할_수_없다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(appPut("/api/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"));
		mockMvc.perform(appGet("/api/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"));
	}

	@Test
	void 정회원도_온보딩_메타데이터를_조회할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(appGet("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	@Test
	void 문서_비활성_환경에서는_토큰_없이_Swagger_UI에_접근하면_403을_반환한다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isForbidden());
	}

	@Test
	void 문서_비활성_환경에서는_유효한_토큰으로도_Swagger_UI에_접근할_수_없다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get("/swagger-ui/index.html")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden());
	}
}
