package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

@ApplicationTest
@WithMockUser
class ChatRegisterTest {
	@Autowired ChatRegister register;
	@Autowired ChatFinder finder;
	@Autowired UserRepository users;
	@Autowired EntityManager em;
	@Autowired JdbcTemplate jdbc;

	private Long member() {
		var user = users.save(User.registerUser());
		em.flush();
		return user.getId();
	}

	@Test
	void 같은_UUID_재전송은_메시지를_중복_저장하지_않는다() {
		Long owner = member();
		register.open(owner);
		var request = new ChatMessageRequest(UUID.randomUUID(), "문의");
		var first = register.send(owner, owner, request);
		var retry = register.send(owner, owner, request);
		assertThat(retry.getId()).isEqualTo(first.getId());
		assertThat(finder.newMessages(owner, 0)).hasSize(1);
	}

	@Test
	void 빈_대화의_커서부터_50건씩_빠짐없이_복구한다() {
		Long owner = member();
		register.open(owner);
		for (int n = 0; n < 61; n++) {
			register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "문의" + n));
		}
		var first = finder.newMessages(owner, 0);
		var next = finder.newMessages(owner, first.getLast().getId());
		assertThat(first).hasSize(50);
		assertThat(first.getFirst().getBody()).isEqualTo("문의0");
		assertThat(next).hasSize(11);
		var latest = finder.latestMessages(owner);
		assertThat(latest.getFirst().getBody()).isEqualTo("문의11");
		assertThat(finder.previousMessages(owner, latest.getFirst().getId())).hasSize(11);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void 관리자_메시지는_상담원_답변으로_저장한다() {
		Long owner = member();
		var admin = User.registerUser();
		ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
		users.save(admin);
		em.flush();
		register.open(owner);
		assertThat(
						register.send(
										admin.getId(),
										owner,
										new ChatMessageRequest(UUID.randomUUID(), "답변"))
								.isOperator())
				.isTrue();

	}

	@Test
	void 새_메시지가_없으면_빈_목록을_반환하고_다음_조회에서_새_메시지만_받는다() {
		Long owner = member();
		register.open(owner);
		var first = register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "첫 문의"));
		assertThat(finder.newMessages(owner, first.getId())).isEmpty();
		var next = register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "추가 문의"));
		assertThat(finder.newMessages(owner, first.getId())).containsExactly(next);
	}

	@Test
	void 새_메시지_조회에_음수_커서를_사용하면_거절한다() {
		Long owner = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.newMessages(owner, -1))
				.isInstanceOf(ChatCursorInvalidException.class);
	}

	@Test
	void 이전_메시지_조회에는_양수_커서가_필요하다() {
		Long owner = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.previousMessages(owner, 0))
				.isInstanceOf(ChatCursorInvalidException.class);
	}

	@Test
	void 오래된_대화에_새_메시지가_오면_목록_맨_앞으로_이동한다() {
		Long owner = member();
		Long other = member();
		var admin = User.registerUser();
		ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
		users.save(admin);
		em.flush();
		register.open(owner);
		register.open(other);
		jdbc.update("UPDATE support_conversations SET updated_at=now()-interval '1 day'");
		register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "새 문의"));
		assertThat(finder.conversations(0, null).getFirst().getId()).isEqualTo(owner);
		assertThat(finder.conversations(owner, finder.conversations(0, null).getFirst().getUpdatedAt()))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(other);
	}
	@Test
	void 활동_시각이_같으면_대화_ID로_이전_목록을_구분한다() {
		Long first = member();
		Long second = member();
		Long third = member();
		var admin = User.registerUser();
		ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
		users.save(admin);
		em.flush();
		Long adminId = admin.getId();
		register.open(first);
		register.open(second);
		register.open(third);
		jdbc.update("UPDATE support_conversations SET updated_at=now()");
		em.clear();

		assertThat(finder.conversations(0, null))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(third, second, first);
		assertThat(finder.conversations(second, finder.conversations(0, null).get(1).getUpdatedAt()))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(first);
		assertThat(finder.conversations(first, finder.conversations(0, null).getLast().getUpdatedAt())).isEmpty();
	}


	@Test
	void 커서_대화가_갱신되어도_이전_조회_경계를_유지한다() {
		Long first = member();
		Long second = member();
		Long third = member();
		register.open(first);
		register.open(second);
		register.open(third);
		jdbc.update("UPDATE support_conversations SET updated_at=now()-interval '1 day'");
		em.clear();
		var cursor = finder.conversations(0, null).get(1);
		var cursorId = cursor.getId();
		var cursorUpdatedAt = cursor.getUpdatedAt();
		register.send(second, second, new ChatMessageRequest(UUID.randomUUID(), "새 문의"));
		em.flush();
		em.clear();

		assertThat(finder.conversations(cursorId, cursorUpdatedAt))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(first);
	}

	@Test
	void 대화_목록_커서는_ID와_시각을_함께_지정한다() {
		assertThatThrownBy(() -> finder.conversations(1, null))
				.isInstanceOf(ChatCursorInvalidException.class);
		assertThatThrownBy(() -> finder.conversations(0, java.time.Instant.now()))
				.isInstanceOf(ChatCursorInvalidException.class);
	}
}
