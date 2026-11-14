package com.daesabu.meongcoach.support.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatMessageTest {
	@Test
	void 상담원_답변은_대화_소유자에게_알림을_보낸다() {
		var message = new ChatMessage(17, 42, 99, true, UUID.randomUUID(), "답변", Instant.now());

		assertThat(message.replySentEvent()).contains(new ChatReplySentEvent(17, 42));
	}

	@Test
	void 회원_메시지는_답변_알림을_만들지_않는다() {
		var message = new ChatMessage(17, 42, 42, false, UUID.randomUUID(), "문의", Instant.now());

		assertThat(message.replySentEvent()).isEmpty();
	}

	@Test
	void 상담원도_자기_대화에_쓴_메시지는_알림을_만들지_않는다() {
		var message = new ChatMessage(17, 42, 42, true, UUID.randomUUID(), "문의", Instant.now());

		assertThat(message.replySentEvent()).isEmpty();
	}
}
