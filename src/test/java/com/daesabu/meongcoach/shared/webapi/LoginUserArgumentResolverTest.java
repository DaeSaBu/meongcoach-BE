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
	@WithLoginUser("42")
	@DisplayName("액세스 토큰의 sub 클레임이 로그인 사용자 식별자로 바인딩된다")
	void bindsUserIdFromTokenSubject() throws Exception {
		mockMvc.perform(get("/test/login-user"))
				.andExpect(status().isOk())
				.andExpect(content().string("42"));
	}

	@Test
	@DisplayName("인증 정보가 없으면 401을 반환한다")
	void missingAuthenticationReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/test/login-user"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@WithLoginUser("not-a-number")
	@DisplayName("sub 클레임이 회원 ID 형식이 아니면 401을 반환한다")
	void nonNumericSubjectReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/test/login-user"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@RestController
	static class LoginUserTriggerController {

		@GetMapping("/test/login-user")
		Long loginUser(@LoginUser Long userId) {
			return userId;
		}
	}
}
