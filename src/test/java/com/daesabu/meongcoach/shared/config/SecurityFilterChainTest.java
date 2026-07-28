package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
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

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

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
		AuthToken token = tokenProvider.issue(1L);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.refreshToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("유효한 액세스 토큰이면 인증을 통과한다")
	void validAccessTokenPassesAuthentication() throws Exception {
		AuthToken token = tokenProvider.issue(1L);

		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isNotFound());
	}
}
