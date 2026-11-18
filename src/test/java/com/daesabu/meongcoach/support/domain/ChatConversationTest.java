package com.daesabu.meongcoach.support.domain;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.domain.exception.ChatClosedException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ChatConversationTest {
	private ChatConversation conversation() {
		return ChatConversationFixture.create(17, 42, LocalDateTime.parse("2026-11-01T00:00:00"));
	}

	@Test
	void 닫으면_종료_시각을_기록한다() {
		var conversation = conversation();

		conversation.close();

		assertThat(conversation.isClosed()).isTrue();
		assertThat(conversation.getClosedAt()).isNotNull();
	}

	@Test
	void 이미_닫힌_대화를_다시_닫아도_처음_종료_시각을_유지한다() {
		var conversation = conversation();
		conversation.close();
		var closedAt = conversation.getClosedAt();

		conversation.close();

		assertThat(conversation.getClosedAt()).isEqualTo(closedAt);
	}

	@Test
	void 진행_중인_대화는_추가_대화를_허용한다() {
		var conversation = conversation();

		assertThatCode(conversation::verifyOpen).doesNotThrowAnyException();
	}

	@Test
	void 닫힌_대화는_추가_대화를_거절한다() {
		var conversation = conversation();
		conversation.close();

		assertThatThrownBy(conversation::verifyOpen).isInstanceOf(ChatClosedException.class);
	}
}
