package com.daesabu.meongcoach.shared.config;

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
@ActiveProfiles({"prod", "test"})
@DisplayName("운영 환경 CORS 구성")
class ProdCorsConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("운영 프론트엔드 origin만 허용한다")
	void onlyProductionFrontendOriginIsPermitted() throws Exception {
		mockMvc.perform(options("/api/training/training-categories")
					.header(HttpHeaders.ORIGIN, "https://app.meongcoach.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.meongcoach.com"));

		mockMvc.perform(options("/api/training/training-categories")
					.header(HttpHeaders.ORIGIN, "https://app.dev.meongcoach.com")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
