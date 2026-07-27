package com.daesabu.meongcoach.shared.webapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(LoginUserArgumentResolverTest.LoginUserTriggerController.class)
@Import(LoginUserArgumentResolverTest.LoginUserTriggerController.class)
@DisplayName("로그인 사용자 해석")
class LoginUserArgumentResolverTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("X-User-Id 헤더 값이 로그인 사용자 식별자로 바인딩된다")
	void bindsUserIdFromHeader() throws Exception {
		mockMvc.perform(get("/test/login-user").header("X-User-Id", "42"))
				.andExpect(status().isOk())
				.andExpect(content().string("42"));
	}

	@Test
	@DisplayName("X-User-Id 헤더가 없으면 400을 반환한다")
	void missingHeaderReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/test/login-user"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	@DisplayName("X-User-Id 헤더 값이 숫자가 아니면 400을 반환한다")
	void nonNumericHeaderReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/test/login-user").header("X-User-Id", "not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@RestController
	static class LoginUserTriggerController {

		@GetMapping("/test/login-user")
		Long loginUser(@LoginUser Long userId) {
			return userId;
		}
	}
}
