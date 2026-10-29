package com.daesabu.meongcoach.shared.config;

import static com.daesabu.meongcoach.support.AppClientRequests.APP_PLATFORM_HEADER;
import static com.daesabu.meongcoach.support.AppClientRequests.APP_VERSION_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "meongcoach.cors.allowed-origin-patterns=http://localhost:*")
@AutoConfigureMockMvc
class CorsConfigTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:3000";

	private static final String PROTECTED_PATH = "/api/dashboard/users";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void 허용한_오리진의_preflight는_앱_헤더_없이도_허용한다() throws Exception {
		mockMvc.perform(options(PROTECTED_PATH)
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
								"authorization,content-type,x-app-platform,x-app-version"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						"GET,POST,PUT,PATCH,DELETE,OPTIONS"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						"authorization, content-type, x-app-platform, x-app-version"));
	}

	@Test
	void 허용하지_않은_오리진의_preflight는_거부한다() throws Exception {
		mockMvc.perform(options(PROTECTED_PATH)
						.header(HttpHeaders.ORIGIN, "https://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void 앱_버전_게이트_실패_응답에도_CORS_헤더를_포함한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
				.andExpect(status().isUpgradeRequired())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
	}

	@Test
	void 인증_실패_응답에도_CORS_헤더를_포함한다() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH)
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.header(APP_PLATFORM_HEADER, "web")
						.header(APP_VERSION_HEADER, "1.0.0"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
	}
}
