package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 부하 테스트 활성 환경(local·dev)의 계정 생성 경로 접근을 검증한다.
 * 비활성(기본값) 경로는 SecurityFilterChainTest가 검증한다.
 */
@SpringBootTest(properties = {"meongcoach.loadtest.enabled=true", "meongcoach.loadtest.api-key=test-loadtest-key"})
@AutoConfigureMockMvc
class LoadTestAccessTest {

	private static final String ACCOUNT_BODY = "{\"email\": \"lt-access@meongcoach.test\", \"password\": \"loadtest-password\"}";

	@Autowired
	private MockMvc mockMvc;

	// 필터 체인이 막았다면 코드가 UNAUTHORIZED 또는 403 본문 없음이다. 도메인 에러 코드가 나오면 컨트롤러까지 도달한 것이다
	@Test
	void 활성_환경에서는_토큰_없이_필터_체인을_통과하고_키_검사는_컨트롤러가_한다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ACCOUNT_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("LOADTEST_INVALID_KEY"));
	}

	@Test
	void 활성_환경에서_올바른_키로_요청하면_계정이_생성된다() throws Exception {
		mockMvc.perform(post("/api/loadtest/accounts")
						.header("X-Loadtest-Key", "test-loadtest-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(ACCOUNT_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").isNumber());
	}
}
