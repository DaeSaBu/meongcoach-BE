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

	@Test
	void 상담_시각은_생성_시각을_보존하고_메시지_전송_시_수정_시각을_갱신한다() {
		Long owner = member();
		long id = register.open(owner).getId();
		jdbc.update("UPDATE support_conversations SET created_at = ?, updated_at = ? WHERE id = ?",
				java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse("2026-01-01T00:00:00")),
				java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse("2026-01-01T00:00:00")), id);
		em.clear();
		var before = finder.findConversation(owner, id);
		var createdAt = before.getCreatedAt();
		var updatedAt = before.getUpdatedAt();

		var message = register.send(owner, id, new ChatMessageRequest(UUID.randomUUID(), "시각 검증"));
		assertThat(em.contains(before)).isTrue();
		em.flush();
		var messageCreatedAt = message.getCreatedAt();
		em.clear();

		var after = finder.findConversation(owner, id);
		assertThat(after.getCreatedAt()).isEqualTo(createdAt);
		assertThat(after.getUpdatedAt()).isAfter(updatedAt);
		assertThat(finder.latestMessages(owner, id).getFirst().getCreatedAt())
				.isCloseTo(messageCreatedAt, within(1, java.time.temporal.ChronoUnit.MICROS));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = {"UTC", "Asia/Seoul", "America/New_York"})
	void 서버_시간대와_관계없이_저장된_시각과_커서를_유지한다(String zone) {
		var original = java.util.TimeZone.getDefault();
		try {
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
			Long owner = member();
			long id = register.open(owner).getId();
			var expected = java.time.LocalDateTime.parse("2026-11-01T05:30:00.123456");
			jdbc.update("UPDATE support_conversations SET created_at = ?, updated_at = ? WHERE id = ?",
					java.sql.Timestamp.valueOf(expected), java.sql.Timestamp.valueOf(expected), id);
			em.clear();

			var conversation = finder.findConversation(owner, id);
			java.time.LocalDateTime createdAt = conversation.getCreatedAt();
			assertThat(createdAt).isEqualTo(expected);
			assertThat(conversation.getUpdatedAt()).isEqualTo(expected);
			assertThat(finder.conversations(id, expected)).isEmpty();
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
			assertThat(conversation.getUpdatedAt()).isEqualTo(expected);
		} finally {
			java.util.TimeZone.setDefault(original);
		}
	}

	@Test
	void 대화_생성과_활동_시각은_JVM과_DB_세션_시간대와_관계없이_DB의_UTC를_사용한다() {
		var original = java.util.TimeZone.getDefault();
		try {
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Honolulu"));
			jdbc.execute("SET LOCAL TIME ZONE 'Asia/Seoul'");
			Long owner = member();
			var start = jdbc.queryForObject("SELECT clock_timestamp() AT TIME ZONE 'UTC'", java.time.LocalDateTime.class);

			var conversation = register.open(owner);
			long id = conversation.getId();
			var createdAt = conversation.getCreatedAt();
			jdbc.update("UPDATE support_conversations SET updated_at = TIMESTAMP '2026-01-01 00:00:00' WHERE id = ?", id);
			em.clear();
			register.send(owner, id, new ChatMessageRequest(UUID.randomUUID(), "DB 시각 확인"));
			em.flush();
			em.clear();

			var end = jdbc.queryForObject("SELECT clock_timestamp() AT TIME ZONE 'UTC'", java.time.LocalDateTime.class);
			var saved = finder.findConversation(owner, id);
			assertThat(createdAt).isBetween(start, end);
			assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
			assertThat(saved.getUpdatedAt()).isBetween(start, end);
		} finally {
			java.util.TimeZone.setDefault(original);
		}
	}

	private long conversationId(Long memberId) {
		return register.open(memberId).getId();
	}

	private Long member() {
		var user = users.save(User.registerUser());
		em.flush();
		return user.getId();
	}

	@Test
	void 회원_ID로_다시_열면_같은_대화를_반환한다() {
		Long owner = member();
		var first = register.open(owner);
		assertThat(first.getMemberId()).isEqualTo(owner);
		assertThat(register.open(owner).getId()).isEqualTo(first.getId());
	}

	@Test
	void 같은_UUID_재전송은_메시지를_중복_저장하지_않는다() {
		Long owner = member();
		register.open(owner);
		var request = new ChatMessageRequest(UUID.randomUUID(), "문의");
		var first = register.send(owner, conversationId(owner), request);
		var retry = register.send(owner, conversationId(owner), request);
		assertThat(retry.getId()).isEqualTo(first.getId());
		assertThat(finder.newMessages(owner, conversationId(owner), 0)).hasSize(1);
	}

	@Test
	void 빈_대화의_커서부터_50건씩_빠짐없이_복구한다() {
		Long owner = member();
		register.open(owner);
		for (int n = 0; n < 61; n++) {
			register.send(owner, conversationId(owner), new ChatMessageRequest(UUID.randomUUID(), "문의" + n));
		}
		var first = finder.newMessages(owner, conversationId(owner), 0);
		var next = finder.newMessages(owner, conversationId(owner), first.getLast().getId());
		assertThat(first).hasSize(50);
		assertThat(first.getFirst().getBody()).isEqualTo("문의0");
		assertThat(next).hasSize(11);
		var latest = finder.latestMessages(owner, conversationId(owner));
		assertThat(latest.getFirst().getBody()).isEqualTo("문의11");
		assertThat(finder.previousMessages(owner, conversationId(owner), latest.getFirst().getId())).hasSize(11);
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
										conversationId(owner),
										new ChatMessageRequest(UUID.randomUUID(), "답변"))
								.isOperator())
				.isTrue();

	}

	@Test
	void 새_메시지가_없으면_빈_목록을_반환하고_다음_조회에서_새_메시지만_받는다() {
		Long owner = member();
		register.open(owner);
		var first = register.send(owner, conversationId(owner), new ChatMessageRequest(UUID.randomUUID(), "첫 문의"));
		assertThat(finder.newMessages(owner, conversationId(owner), first.getId())).isEmpty();
		var next = register.send(owner, conversationId(owner), new ChatMessageRequest(UUID.randomUUID(), "추가 문의"));
		assertThat(finder.newMessages(owner, conversationId(owner), first.getId())).containsExactly(next);
	}

	@Test
	void 새_메시지_조회에_음수_커서를_사용하면_거절한다() {
		Long owner = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.newMessages(owner, conversationId(owner), -1))
				.isInstanceOf(ChatCursorInvalidException.class);
	}

	@Test
	void 이전_메시지_조회에는_양수_커서가_필요하다() {
		Long owner = member();
		register.open(owner);
		assertThatThrownBy(() -> finder.previousMessages(owner, conversationId(owner), 0))
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
		register.send(owner, conversationId(owner), new ChatMessageRequest(UUID.randomUUID(), "새 문의"));
		em.flush();
		em.clear();
		assertThat(finder.conversations(0, null).getFirst().getId()).isEqualTo(conversationId(owner));
		assertThat(finder.conversations(conversationId(owner), finder.conversations(0, null).getFirst().getUpdatedAt()))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(conversationId(other));
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
				.containsExactly(conversationId(third), conversationId(second), conversationId(first));
		assertThat(finder.conversations(conversationId(second), finder.conversations(0, null).get(1).getUpdatedAt()))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(conversationId(first));
		assertThat(finder.conversations(conversationId(first), finder.conversations(0, null).getLast().getUpdatedAt())).isEmpty();
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
		register.send(second, conversationId(second), new ChatMessageRequest(UUID.randomUUID(), "새 문의"));
		em.flush();
		em.clear();

		assertThat(finder.conversations(cursorId, cursorUpdatedAt))
				.extracting(com.daesabu.meongcoach.support.domain.ChatConversation::getId)
				.containsExactly(conversationId(first));
	}

	@Test
	void 대화_목록_커서는_ID와_시각을_함께_지정한다() {
		assertThatThrownBy(() -> finder.conversations(1, null))
				.isInstanceOf(ChatCursorInvalidException.class);
		assertThatThrownBy(() -> finder.conversations(0, java.time.LocalDateTime.now()))
				.isInstanceOf(ChatCursorInvalidException.class);
	}
}
