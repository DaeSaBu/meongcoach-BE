package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.support.application.required.ChatConversationRepository;
import com.daesabu.meongcoach.support.domain.ChatConversation;
import com.daesabu.meongcoach.support.domain.exception.ChatCursorInvalidException;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@ApplicationTest
@WithMockUser
class ChatFinderTest {
	@Autowired ChatFinder finder;
	@Autowired ChatRegister register;
	@Autowired ChatRemover remover;
	@Autowired ChatConversationRepository conversations;
	@Autowired UserRepository users;

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
	void 회원_대화_목록의_다음_페이지는_이전_응답_마지막_대화_이후를_조회한다() {
		Long owner = users.save(User.registerUser()).getId();
		long firstId = register.open(owner).getId();
		register.send(owner, firstId, new ChatMessageRequest(UUID.randomUUID(), "첫 문의"));
		remover.close(owner, firstId);
		long secondId = register.open(owner).getId();
		register.send(owner, secondId, new ChatMessageRequest(UUID.randomUUID(), "두 번째 문의"));
		conversations.flush();
		var last = finder.memberConversations(owner, 0, null).getFirst();

		var next = finder.memberConversations(owner, last.getId(), last.getLastMessageAt());

		assertThat(next).extracting(ChatConversation::getId).containsExactly(firstId);
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
