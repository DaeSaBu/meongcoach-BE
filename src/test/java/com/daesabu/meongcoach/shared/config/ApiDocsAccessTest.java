package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
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
@DisplayName("API 문서 접근")
class ApiDocsAccessTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("문서 활성 환경에서는 인증 없이 Swagger UI 페이지를 볼 수 있다")
	void swaggerUiIsPermittedWhenEnabled() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	// openapi3.json은 빌드 산출물이라 실행 시점에 따라 200(생성됨) 또는 404(미생성)다.
	// 이 테스트는 파일 유무가 아니라 보안 통과(401·403이 아님)만 검증한다
	@Test
	@DisplayName("문서 활성 환경에서는 스펙 경로도 인증 없이 통과된다")
	void openApiSpecPathIsPermittedWhenEnabled() throws Exception {
		mockMvc.perform(get("/swagger-ui/openapi3.json"))
				.andExpect(result -> {
					int status = result.getResponse().getStatus();
					if (status == 401 || status == 403) {
						throw new AssertionError("보안에 막혔습니다: status=" + status);
					}
				});
	}
}
