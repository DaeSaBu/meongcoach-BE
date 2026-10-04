package com.daesabu.meongcoach.shared.config;

import static com.daesabu.meongcoach.support.AppClientRequests.APP_PLATFORM_HEADER;
import static com.daesabu.meongcoach.support.AppClientRequests.APP_VERSION_HEADER;
import static com.daesabu.meongcoach.support.AppClientRequests.appGet;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AppVersionGateTest {

	private static final String PROTECTED_PATH = "/api/dogs";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 앱_헤더_없이_API를_호출하면_Problem_Details로_426을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
				.andExpect(status().isUpgradeRequired())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void 앱_버전만_있고_플랫폼이_없으면_426을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header(APP_VERSION_HEADER, "2.0.0"))
				.andExpect(status().isUpgradeRequired())
				.andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"));
	}

	@Test
	void 최소_지원_버전보다_낮으면_426을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)
						.header(APP_PLATFORM_HEADER, "android")
						.header(APP_VERSION_HEADER, "1.9.9"))
				.andExpect(status().isUpgradeRequired())
				.andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"));
	}

	@Test
	void 앱_버전_형식이_틀리면_400을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)
						.header(APP_PLATFORM_HEADER, "ios")
						.header(APP_VERSION_HEADER, "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("APP_VERSION_INVALID"));
	}

	@Test
	void 지원하지_않는_플랫폼이면_400을_반환한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)
						.header(APP_PLATFORM_HEADER, "web")
						.header(APP_VERSION_HEADER, "2.0.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("APP_VERSION_INVALID"));
	}

	@Test
	void 지원하는_버전이면_게이트를_통과해_인증_단계로_넘어간다() throws Exception {
		mockMvc.perform(appGet(PROTECTED_PATH))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 인증_없이_열린_로그인_경로도_앱_헤더가_없으면_426을_반환한다() throws Exception {
		mockMvc.perform(post("/api/auth/login/email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"nobody@meongcoach.com\", \"password\": \"wrong-password\"}"))
				.andExpect(status().isUpgradeRequired())
				.andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"));
	}

	@Test
	void 헬스_체크는_앱_헤더_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}
}
