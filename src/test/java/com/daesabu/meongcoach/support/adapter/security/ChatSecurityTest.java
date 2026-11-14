package com.daesabu.meongcoach.support.adapter.security;

import static com.daesabu.meongcoach.support.AppClientRequests.appGet;
import static com.daesabu.meongcoach.support.AppClientRequests.appPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
	@MockitoSpyBean RegisteredUserChecker registeredUsers;

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

	private String messages(Long conversationId) {
		return "/api/support/conversations/" + conversationId + "/messages";
	}

	private String body() {
		return "{\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"body\":\"문의\"}";
	}

	@Test
	void 회원은_자기_대화를_열고_조회하고_전송할_수_있다() throws Exception {
		Long owner = member(UserRole.USER);
		String token = bearer(owner);
		long conversationId = chats.open(owner).getId();
		mvc.perform(appPost("/api/support/conversations/me").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(conversationId)).andExpect(jsonPath("$.memberId").value(owner));
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(false));
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(conversationId) + "/new").param("after", "0").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(conversationId) + "/previous").param("before", "1").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet("/api/support/conversations/capabilities").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(false));
	}

	@Test
	void 다른_회원의_대화와_관리자_목록은_거절한다() throws Exception {
		Long owner = member(UserRole.USER);
		long conversationId = chats.open(owner).getId();
		String token = bearer(member(UserRole.USER));
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SUPPORT_CHAT_ACCESS_DENIED"));
		mvc.perform(appGet(messages(conversationId) + "/new").param("after", "0").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appGet(messages(conversationId) + "/previous").param("before", "1").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isForbidden());
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isForbidden());
		assertThat(finder.latestMessages(owner, conversationId)).isEmpty();
	}

	@Test
	void 대화_ID가_다른_회원_ID와_같아도_소유자만_접근한다() throws Exception {
		Long owner = member(UserRole.USER);
		Long attacker = member(UserRole.USER);
		em.createNativeQuery("INSERT INTO support_conversations (id, member_id, updated_at) VALUES (:id, :memberId, now())")
				.setParameter("id", attacker).setParameter("memberId", owner).executeUpdate();
		String ownerToken = bearer(owner);
		String attackerToken = bearer(attacker);
		assertThat(chats.open(owner).getId()).isEqualTo(attacker);
		for (String path : java.util.List.of(messages(attacker), messages(attacker) + "/new?after=0", messages(attacker) + "/previous?before=1")) {
			mvc.perform(appGet(path).header("Authorization", ownerToken)).andExpect(status().isOk());
			mvc.perform(appGet(path).header("Authorization", attackerToken)).andExpect(status().isForbidden());
		}
		mvc.perform(appPost(messages(attacker)).header("Authorization", attackerToken)
				.contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
		mvc.perform(appPost(messages(attacker)).header("Authorization", ownerToken)
				.contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk());
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"ADMIN", "EXPERT"})
	void 상담원는_다른_회원에게_답변하고_목록을_조회한다(UserRole role) throws Exception {
		Long owner = member(UserRole.USER);
		long conversationId = chats.open(owner).getId();
		String token = bearer(member(role));
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(conversationId) + "/new").param("after", "0").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet(messages(conversationId) + "/previous").param("before", "1").header("Authorization", token))
				.andExpect(status().isOk());
		mvc.perform(appGet("/api/support/conversations/capabilities").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"ADMIN", "EXPERT"})
	void 상담원_권한을_잃으면_기존_토큰으로도_다른_대화에_접근할_수_없다(UserRole role) throws Exception {
		Long owner = member(UserRole.USER);
		long conversationId = chats.open(owner).getId();
		Long adminId = member(role);
		String token = bearer(adminId);
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token)).andExpect(status().isOk());
		ReflectionTestUtils.setField(users.findById(adminId).orElseThrow(), "role", UserRole.USER);
		em.flush();
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token)).andExpect(status().isForbidden());
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
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
		long conversationId = chats.open(owner).getId();
		String token = bearer(owner);
		users.findById(owner).orElseThrow().withdraw();
		em.flush();
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token))
				.andExpect(status().isUnauthorized());
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 온보딩_미완료_회원은_자기_대화도_접근할_수_없다() throws Exception {
		Long owner = member(UserRole.ONBOARDING_USER);
		long conversationId = chats.open(owner).getId();
		String token = bearer(owner);
		mvc.perform(appGet(messages(conversationId)).header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isForbidden());
		mvc.perform(appGet("/api/support/conversations").header("Authorization", token))
				.andExpect(status().isForbidden());
		mvc.perform(appPost("/api/support/conversations/me").header("Authorization", token))
				.andExpect(status().isForbidden());
	}
	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"ADMIN", "EXPERT"})
	void 상담원_조회는_인증_단계의_권한을_재사용한다(UserRole role) throws Exception {
		Long adminId = member(role);
		String token = bearer(adminId);
		mvc.perform(appGet("/api/support/conversations/capabilities").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
		verify(registeredUsers, times(1)).findRole(adminId);
	}

	@ParameterizedTest
	@EnumSource(value = UserRole.class, names = {"ADMIN", "EXPERT"})
	void 메시지_작성도_인증_단계의_권한을_재사용한다(UserRole role) throws Exception {
		Long owner = member(UserRole.USER);
		long conversationId = chats.open(owner).getId();
		Long adminId = member(role);
		String token = bearer(adminId);
		mvc.perform(appPost(messages(conversationId)).header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content(body()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.operator").value(true));
		verify(registeredUsers, times(1)).findRole(adminId);
	}

}
