package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

@ApplicationTest
@Sql({"/db/migration/V50__create_support_chat.sql", "/db/migration/V51__create_support_push_jobs.sql", "/db/migration/V52__drop_support_socket_tickets.sql", "/db/migration/V53__drop_support_push_jobs.sql"})
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
	void 다른_회원은_조회와_발송을_할_수_없다() {
		Long owner = member();
		Long stranger = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.messages(stranger, owner, -1, 0))
				.isInstanceOf(ChatAccessDeniedException.class);
		assertThatThrownBy(
						() ->
								register.send(
										stranger,
										owner,
										new ChatMessageRequest(UUID.randomUUID(), "문의")))
				.isInstanceOf(ChatAccessDeniedException.class);
	}

	@Test
	void 같은_UUID_재전송은_메시지를_중복_저장하지_않는다() {
		Long owner = member();
		register.open(owner);
		var request = new ChatMessageRequest(UUID.randomUUID(), "문의");
		var first = register.send(owner, owner, request);
		var retry = register.send(owner, owner, request);
		assertThat(retry.id()).isEqualTo(first.id());
		assertThat(finder.messages(owner, owner, 0, 0)).hasSize(1);
	}

	@Test
	void 빈_대화의_커서부터_50건씩_빠짐없이_복구한다() {
		Long owner = member();
		register.open(owner);
		for (int n = 0; n < 61; n++) {
			register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "문의" + n));
		}
		var first = finder.messages(owner, owner, 0, 0);
		var next = finder.messages(owner, owner, first.getLast().id(), 0);
		assertThat(first).hasSize(50);
		assertThat(first.getFirst().body()).isEqualTo("문의0");
		assertThat(next).hasSize(11);
		var latest = finder.messages(owner, owner, -1, 0);
		assertThat(latest.getFirst().body()).isEqualTo("문의11");
		assertThat(finder.messages(owner, owner, -1, latest.getFirst().id())).hasSize(11);
	}

	@Test
	void 관리자는_답변할_수_있고_권한을_잃으면_접근할_수_없다() {
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
								.operator())
				.isTrue();
		ReflectionTestUtils.setField(admin, "role", UserRole.USER);
		em.flush();
		assertThatThrownBy(() -> finder.messages(admin.getId(), owner, -1, 0))
				.isInstanceOf(ChatAccessDeniedException.class);
	}

	@Test
	void 새_메시지가_없으면_빈_목록을_반환하고_다음_조회에서_새_메시지만_받는다() {
		Long owner = member();
		register.open(owner);
		var first = register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "첫 문의"));
		assertThat(finder.messages(owner, owner, first.id(), 0)).isEmpty();
		var next = register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "추가 문의"));
		assertThat(finder.messages(owner, owner, first.id(), 0)).containsExactly(next);
	}

	@Test
	void 탈퇴한_회원은_메시지를_조회할_수_없다() {
		Long owner = member();
		register.open(owner);
		users.findById(owner).orElseThrow().withdraw();
		em.flush();
		assertThatThrownBy(() -> finder.messages(owner, owner, 0, 0))
				.isInstanceOf(ChatAccessDeniedException.class);
	}

	@Test
	void 양방향_커서를_동시에_사용하면_거절한다() {
		Long owner = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.messages(owner, owner, 0, 1))
				.isInstanceOf(
						com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException
								.class);
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
		assertThat(finder.conversations(admin.getId(), 0).getFirst().id()).isEqualTo(owner);
		assertThat(finder.conversations(admin.getId(), owner))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::id)
				.containsExactly(other);
	}
}
