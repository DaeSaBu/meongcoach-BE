package com.daesabu.meongcoach.support.adapter.security;

import static com.daesabu.meongcoach.support.AppClientRequests.appGet;
import static com.daesabu.meongcoach.support.AppClientRequests.appPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@ApplicationTest
@AutoConfigureMockMvc
class ChatSecurityTest {
	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired ChatRegister chats;
	@Autowired ChatFinder finder;
	@Autowired TokenProvider tokens;
	@Autowired EntityManager em;

	private Long member(UserRole role) {
		var user = User.registerUser();
		ReflectionTestUtils.setField(user, "role", role);
		Long id = users.save(user).getId();
		em.flush();
		return id;
	}

	private String bearer(Long userId) {
		return "Bearer " + tokens.issue(userId).accessToken();
	}

	private String messages(Long userId) {
		return "/api/support/conversations/" + userId + "/messages";
	}

	private String body() {
		return "{\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"body\":\"문의\"}";
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"USER", "EXPERT"})
	void 회원은_자기_대화를_열고_조회하고_전송할_수_있다(UserRole role) throws Exception {
		Long owner = member(role);
		String token = bearer(owner);
		mvc.perform(appPost("/api/support/conversations/me").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner));
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(false));
		mvc.perform(appGet(messages(owner)).header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(owner)).param("after", "0").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(owner)).param("before", "1").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet("/api/support/conversations/capabilities").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(false));
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"USER", "EXPERT"})
	void 다른_회원의_대화와_관리자_목록은_거절한다(UserRole role) throws Exception {
		Long owner = member(UserRole.USER);
		chats.open(owner);
		String token = bearer(member(role));
		mvc.perform(appGet(messages(owner)).header("Authorization", token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mvc.perform(appGet(messages(owner)).param("after", "0").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appGet(messages(owner)).param("before", "1").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isForbidden());
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isForbidden());
		assertThat(finder.latestMessages(owner)).isEmpty();
	}

	@Test
	void 관리자는_다른_회원에게_답변하고_목록을_조회한다() throws Exception {
		Long owner = member(UserRole.USER);
		chats.open(owner);
		String token = bearer(member(UserRole.ADMIN));
		mvc.perform(appGet(messages(owner)).header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet("/api/support/conversations/capabilities").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
	}

	@Test
	void 관리자_권한을_잃으면_기존_토큰으로도_다른_대화에_접근할_수_없다() throws Exception {
		Long owner = member(UserRole.USER);
		chats.open(owner);
		Long adminId = member(UserRole.ADMIN);
		String token = bearer(adminId);
		mvc.perform(appGet(messages(owner)).header("Authorization", token)).andExpect(status().isOk());
		ReflectionTestUtils.setField(users.findById(adminId).orElseThrow(), "role", UserRole.USER);
		em.flush();
		mvc.perform(appGet(messages(owner)).header("Authorization", token)).andExpect(status().isForbidden());
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isForbidden());
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isForbidden());
	}

	@Test
	void 인증이_없으면_채팅에_접근할_수_없다() throws Exception {
		mvc.perform(appGet(messages(1L))).andExpect(status().isUnauthorized());
		mvc.perform(appPost(messages(1L)).contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isUnauthorized());
		mvc.perform(appGet("/api/support/conversations")).andExpect(status().isUnauthorized());
		mvc.perform(appPost("/api/support/conversations/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void 탈퇴한_회원은_기존_토큰으로도_접근할_수_없다() throws Exception {
		Long owner = member(UserRole.USER);
		chats.open(owner);
		String token = bearer(owner);
		users.findById(owner).orElseThrow().withdraw();
		em.flush();
		mvc.perform(appGet(messages(owner)).header("Authorization", token))
				.andExpect(status().isUnauthorized());
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 온보딩_미완료_회원은_자기_대화도_접근할_수_없다() throws Exception {
		Long owner = member(UserRole.ONBOARDING_USER);
		chats.open(owner);
		String token = bearer(owner);
		mvc.perform(appGet(messages(owner)).header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost(messages(owner)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isForbidden());
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost("/api/support/conversations/me").header("Authorization", token))
				.andExpect(status().isForbidden());
	}
}
