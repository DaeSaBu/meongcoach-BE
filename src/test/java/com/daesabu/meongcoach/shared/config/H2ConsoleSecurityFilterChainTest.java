package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * local 프로파일에서만 등록되는 h2-console 전용 체인을 검증한다.
 * test를 뒤에 나열해 test의 datasource·더미 시크릿이 local 값을 덮도록 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@DisplayName("h2-console 시큐리티 필터 체인 (local)")
class H2ConsoleSecurityFilterChainTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("local 프로파일에서는 h2-console 경로가 인증 없이 통과한다")
	void h2ConsoleIsPermittedInLocal() throws Exception {
		// permitAll 체인을 통과했지만 MockMvc에는 콘솔 서블릿이 등록되지 않아 404가 난다.
		// 401이 아니라는 것이 곧 시큐리티 체인을 통과했다는 증거다
		mockMvc.perform(get("/h2-console"))
				.andExpect(status().isNotFound());
	}
}
