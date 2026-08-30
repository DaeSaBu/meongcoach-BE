package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 문서 활성 환경(local·dev)의 Swagger UI 접근을 검증한다.
 * 비활성(기본값) 경로는 SecurityFilterChainTest가 검증한다.
 */
@SpringBootTest(properties = "meongcoach.api-docs.enabled=true")
@AutoConfigureMockMvc
class ApiDocsAccessTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 문서_활성_환경에서는_인증_없이_Swagger_UI_페이지를_볼_수_있다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	// openapi3.json은 빌드 산출물이라 실행 시점에 따라 200(생성됨) 또는 404(미생성)다.
	// 이 테스트는 파일 유무가 아니라 보안 통과(401·403이 아님)만 검증한다
	@Test
	void 문서_활성_환경에서는_스펙_경로도_인증_없이_통과된다() throws Exception {
		mockMvc.perform(get("/swagger-ui/openapi3.json"))
				.andExpect(result -> {
					int status = result.getResponse().getStatus();
					if (status == 401 || status == 403) {
						throw new AssertionError("보안에 막혔습니다: status=" + status);
					}
				});
	}
}
