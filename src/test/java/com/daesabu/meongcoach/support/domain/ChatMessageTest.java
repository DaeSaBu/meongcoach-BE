package com.daesabu.meongcoach.support.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatMessageTest {
	@Test
	void 상담원_답변은_대화_소유자에게_알림을_보낸다() {
		var message = ChatMessageFixture.create(17, 42, 99, true, UUID.randomUUID(), "답변", Instant.now());

		assertThat(message.sentEvent()).isEqualTo(new ChatMessageSentEvent(17, 42, 99));
	}

	@Test
	void 회원_메시지도_발신자를_포함한_발송_이벤트를_만든다() {
		var message = ChatMessageFixture.create(17, 42, 42, false, UUID.randomUUID(), "문의", Instant.now());

		assertThat(message.sentEvent()).isEqualTo(new ChatMessageSentEvent(17, 42, 42));
	}

	@Test
	void 상담원도_자기_대화에_쓴_메시지의_발송_이벤트를_만든다() {
		var message = ChatMessageFixture.create(17, 42, 42, true, UUID.randomUUID(), "문의", Instant.now());

		assertThat(message.sentEvent()).isEqualTo(new ChatMessageSentEvent(17, 42, 42));
	}
}
