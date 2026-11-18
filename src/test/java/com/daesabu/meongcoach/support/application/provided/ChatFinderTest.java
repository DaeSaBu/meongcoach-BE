package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

@ApplicationTest
@WithMockUser
class ChatFinderTest {
	@Autowired ChatFinder finder;
	@Autowired ChatRegister register;
	@Autowired ChatRemover remover;
	@Autowired ChatConversationRepository conversations;
	@Autowired UserRepository users;
	@Autowired JdbcTemplate jdbc;
	@Autowired EntityManager em;

	@Test
	void 회원은_진행_중인_대화와_닫힌_대화를_최근_활동순으로_조회한다() {
		Long owner = users.save(User.registerUser()).getId();
		Long other = users.save(User.registerUser()).getId();
		long closedId = register.open(owner).getId();
		register.send(owner, closedId, new ChatMessageRequest(UUID.randomUUID(), "지난 문의"));
		remover.close(owner, closedId);
		long openId = register.open(owner).getId();
		register.send(owner, openId, new ChatMessageRequest(UUID.randomUUID(), "새 문의"));
		register.open(other);
		conversations.flush();

		var result = finder.memberConversations(owner, 0, null);

		assertThat(result).extracting(ChatConversation::getId).containsExactly(openId, closedId);
		assertThat(result).extracting(ChatConversation::isClosed).containsExactly(false, true);
	}

	@Test
	void 회원_대화_목록의_다음_페이지는_같은_시각이면_ID로_이어서_본인_대화만_조회한다() {
		Long owner = users.save(User.registerUser()).getId();
		Long other = users.save(User.registerUser()).getId();
		long firstId = register.open(owner).getId();
		remover.close(owner, firstId);
		long secondId = register.open(owner).getId();
		remover.close(owner, secondId);
		long otherId = register.open(other).getId();
		long latestId = register.open(owner).getId();
		conversations.flush();
		em.clear();
		LocalDateTime tied = LocalDateTime.parse("2026-11-01T00:00:00.123456");
		LocalDateTime latest = tied.plusSeconds(1);
		setLastMessageAt(tied, firstId, secondId, otherId);
		setLastMessageAt(latest, latestId);

		var afterLatest = finder.memberConversations(owner, latestId, latest);
		var afterSecond = finder.memberConversations(owner, secondId, tied);

		assertThat(afterLatest).extracting(ChatConversation::getId).containsExactly(secondId, firstId);
		assertThat(afterSecond).extracting(ChatConversation::getId).containsExactly(firstId);
	}

	@Test
	void 상담원_목록의_다음_페이지도_닫힌_대화를_제외한다() {
		Long first = users.save(User.registerUser()).getId();
		Long second = users.save(User.registerUser()).getId();
		Long third = users.save(User.registerUser()).getId();
		long oldestId = register.open(first).getId();
		long closedId = register.open(second).getId();
		remover.close(second, closedId);
		long latestId = register.open(third).getId();
		conversations.flush();
		em.clear();
		LocalDateTime latest = LocalDateTime.parse("2999-01-01T00:00:00");
		setLastMessageAt(latest.minusSeconds(2), oldestId);
		setLastMessageAt(latest.minusSeconds(1), closedId);
		setLastMessageAt(latest, latestId);

		var next = finder.conversations(latestId, latest);

		assertThat(next).extracting(ChatConversation::getId).doesNotContain(closedId, latestId).startsWith(oldestId);
	}

	private void setLastMessageAt(LocalDateTime lastMessageAt, long... ids) {
		for (long id : ids) {
			jdbc.update("UPDATE support_conversations SET last_message_at = ? WHERE id = ?", lastMessageAt, id);
		}
	}

	@Test
	void 회원_대화_목록_커서는_ID와_시각을_함께_전달해야_한다() {
		Long owner = users.save(User.registerUser()).getId();

		assertThatThrownBy(() -> finder.memberConversations(owner, 1, null))
				.isInstanceOf(ChatCursorInvalidException.class);
		assertThatThrownBy(() -> finder.memberConversations(owner, 0, LocalDateTime.now()))
				.isInstanceOf(ChatCursorInvalidException.class);
	}

	@Test
	void 대화가_없는_회원은_빈_목록을_받는다() {
		Long owner = users.save(User.registerUser()).getId();

		assertThat(finder.memberConversations(owner, 0, null)).isEmpty();
	}
}
