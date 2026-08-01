package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@DisplayName("로컬 CORS 구성")
class LocalCorsConfigTest {

	private static final String ORIGIN = "http://127.0.0.1:8083";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("로컬 웹 프론트엔드의 preflight 요청을 허용한다")
	void preflightIsPermitted() throws Exception {
		mockMvc.perform(options("/api/training/training-categories")
					.header(HttpHeaders.ORIGIN, ORIGIN)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						"GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	@DisplayName("인증 실패 응답에도 CORS 헤더를 포함한다")
	void unauthorizedResponseIncludesCorsHeader() throws Exception {
		mockMvc.perform(get("/api/training/training-categories").header(HttpHeaders.ORIGIN, ORIGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
	}
}
