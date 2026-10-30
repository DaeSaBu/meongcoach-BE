package com.daesabu.meongcoach.notification.adapter.security;

import static com.daesabu.meongcoach.support.AppClientRequests.appGet;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InternalSecurityFilterChainTest {

	private static final String DISPATCH_PATH = "/internal/notifications/dispatch";
	private static final String DISPATCH_BODY = "{\"scheduledTime\":\"2026-10-31T08:00:00Z\",\"hour\":24}";
	private static final String APP_PREFERENCES_PATH = "/api/notifications/preferences";
	private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
	private static final String INTERNAL_SECRET = "test-internal-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 비밀_헤더가_없으면_Problem_Details로_401을_반환한다() throws Exception {
		mockMvc.perform(post(DISPATCH_PATH).contentType(MediaType.APPLICATION_JSON).content(DISPATCH_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 비밀_헤더_값이_다르면_401을_반환한다() throws Exception {
		mockMvc.perform(post(DISPATCH_PATH)
						.contentType(MediaType.APPLICATION_JSON).content(DISPATCH_BODY)
						.header(INTERNAL_SECRET_HEADER, "wrong-secret"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 비밀_헤더가_맞으면_앱_헤더와_토큰_없이_발송_요청을_검증한다() throws Exception {
		mockMvc.perform(post(DISPATCH_PATH)
						.contentType(MediaType.APPLICATION_JSON).content(DISPATCH_BODY)
						.header(INTERNAL_SECRET_HEADER, INTERNAL_SECRET))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_HOUR_INVALID"));
	}

	@Test
	void 유효한_액세스_토큰으로는_내부_API를_호출할_수_없다() throws Exception {
		AuthToken token = tokenProvider.issue(saveUser().getId());

		mockMvc.perform(post(DISPATCH_PATH)
						.contentType(MediaType.APPLICATION_JSON).content(DISPATCH_BODY)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 비밀_헤더로는_앱_API를_호출할_수_없다() throws Exception {
		mockMvc.perform(appGet(APP_PREFERENCES_PATH).header(INTERNAL_SECRET_HEADER, INTERNAL_SECRET))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 액세스_토큰이_있으면_앱_알림_API를_호출할_수_있다() throws Exception {
		AuthToken token = tokenProvider.issue(saveUser().getId());

		mockMvc.perform(appGet(APP_PREFERENCES_PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hours.length()").value(0));
	}

	private User saveUser() {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand("멍멍이집사", null, "INFP", Set.of(), Set.of()));
		return userRepository.save(user);
	}

}
