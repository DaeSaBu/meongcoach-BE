package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class SecurityFilterChainTest {

	// MEMBER 역할만 허용되는 보호 경로. 인증 없으면 401, 인증되면 보유 강아지가 없어 빈 목록과 200이 나온다
	private static final String PROTECTED_PATH = "/api/dogs";

	// @CurrentUserId를 받는 보호 경로. 없는 토픽을 골라 두어 토픽 조회 단계에서 404로 끝난다
	private static final String CURRENT_USER_PATH = "/api/training/topic/selection";

	private static final String MISSING_TOPIC_SELECTION_BODY = "{\"topicId\": 999}";

	// 저장된 회원 ID에 더해 존재하지 않는 회원 ID를 만든다
	private static final long UNREGISTERED_ID_OFFSET = 1_000_000L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	// 액세스 토큰 검증이 회원 존재·역할을 확인하므로 실제 회원 행이 있어야 인증을 통과한다
	private Long userId;

	// 온보딩 미완료 회원은 허용 경로 밖에서 403을 받아야 하므로 별도로 만든다
	private Long onboardingUserId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(promotedMember()).getId();
		onboardingUserId = userRepository.save(User.registerOnboardingMember()).getId();
	}

	// 정회원은 프로덕션과 동일하게 온보딩 회원 승격 경로로 만든다
	private User promotedMember() {
		User user = User.registerOnboardingMember();
		user.promoteToMember();
		return user;
	}

	@Test
	void 헬스_체크는_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void 토큰_재발급은_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/token/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 로그아웃은_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\": \"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void 잘못된_소셜_토큰을_제출하면_401을_반환한다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(post("/api/auth/login/social/kakao")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\": \"invalid\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"));
	}

	@Test
	void 애플_로그인_경로도_인증_없이_열려_있고_잘못된_토큰이면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/social/apple")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\": \"invalid\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_SOCIAL_TOKEN"));
	}

	// 필터 체인이 막았다면 코드가 UNAUTHORIZED다. 도메인 에러 코드가 나오면 컨트롤러까지 도달한 것이다
	@Test
	void 이메일_로그인_경로는_인증_없이_열려_있고_자격증명이_틀리면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/local")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"nobody@meongcoach.com\", \"password\": \"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"));
	}

	@Test
	void 회원_경로는_인증이_필요하다() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 토큰_없이_보호된_경로에_접근하면_Problem_Details로_401을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void 위조된_토큰은_거부된다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 리프레시_토큰은_액세스_토큰_자리에서_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.refreshToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 유효한_액세스_토큰이면_인증을_통과한다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	// 서명이 유효해도 회원 행이 없으면 통과시키지 않는다. DB가 초기화된 뒤 남아 있는 토큰이 이 경우다
	@Test
	void 등록되지_않은_회원의_액세스_토큰은_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId + UNREGISTERED_ID_OFFSET);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로, 필터 체인이 세운 인증 주체를 CurrentUserIdArgumentResolver가
	// 실제로 읽어내는지는 여기에서만 확인할 수 있다. 401이 아니라 404면 해석에 성공한 것이다
	@Test
	void 필터_체인이_세운_인증_주체가_CurrentUserId_파라미터로_해석된다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(put(CURRENT_USER_PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MISSING_TOPIC_SELECTION_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_TOPIC_NOT_FOUND"));
	}

	@Test
	void 토큰_없이_CurrentUserId_경로에_접근하면_401을_반환한다() throws Exception {
		mockMvc.perform(put(CURRENT_USER_PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(MISSING_TOPIC_SELECTION_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 온보딩_미완료_회원이_정회원_전용_경로에_접근하면_ONBOARDING_NOT_COMPLETED_403을_반환한다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(get("/api/training/curriculums")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"));
	}

	@Test
	void 온보딩_미완료_회원도_온보딩_메타데이터를_조회할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(get("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	// 온보딩 화면이 쓰는 프로필 이미지 조회가 인가에서 걸리지 않는지 확인한다.
	// 강아지가 없는 회원이라 404로 끝나면 403 없이 컨트롤러까지 도달한 것이다
	@Test
	void 온보딩_미완료_회원도_강아지_프로필_이미지_경로에_접근할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(get("/api/dogs/profile/image")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNotFound());
	}

	// 심사관이 온보딩을 마치지 않고 탈퇴할 수 있으므로 탈퇴 경로는 온보딩 미완료 회원에게도 열려 있어야 한다.
	// 탈퇴 뒤에는 같은 토큰이 등록 확인에서 걸려 401로 끝나는지도 확인한다
	@Test
	void 온보딩_미완료_회원도_탈퇴할_수_있고_탈퇴_후_같은_토큰은_거부된다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(delete("/api/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized());
	}

	// 탈퇴는 DELETE만 열었으므로 같은 경로의 다른 메서드는 정회원 규칙을 그대로 따른다
	@Test
	void 온보딩_미완료_회원은_탈퇴_외의_회원_경로에_접근할_수_없다() throws Exception {
		AuthToken token = tokenProvider.issue(onboardingUserId);

		mockMvc.perform(get("/api/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ONBOARDING_NOT_COMPLETED"));
	}

	// 온보딩 허용 경로가 hasAnyRole로 정회원까지 포함하는지 확인한다
	@Test
	void 정회원도_온보딩_메타데이터를_조회할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get("/api/onboarding/metadata")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk());
	}

	// test 프로파일은 meongcoach.api-docs.enabled를 두지 않아 기본값(false) 경로가 검증된다.
	// denyAll은 인증 여부와 무관하게 AccessDeniedException으로 끝나므로 미인증도 401이 아닌 403이다
	@Test
	void 문서_비활성_환경에서는_토큰_없이_Swagger_UI에_접근하면_403을_반환한다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isForbidden());
	}

	// denyAll이 authenticated보다 우선함을 증명한다. 유효 토큰 소지자도 문서를 볼 수 없어야 한다
	@Test
	void 문서_비활성_환경에서는_유효한_토큰으로도_Swagger_UI에_접근할_수_없다() throws Exception {
		AuthToken token = tokenProvider.issue(userId);

		mockMvc.perform(get("/swagger-ui/index.html")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isForbidden());
	}
}
