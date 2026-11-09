package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SupportReplyTest {
	@Test
	void 같은_환경의_회원_ID를_복원한다() {
		assertThat(SupportReply.from("dev-42", "message-1", "dev")).contains(new SupportReply(42L, "message-1"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"prod-42", "42", "dev-0", "dev--1", "dev-01", "dev-1.5", "dev-9223372036854775808", "dev-42/other"})
	void 다른_환경과_잘못된_회원_ID는_건너뛴다(String memberId) {
		assertThat(SupportReply.from(memberId, "message-1", "dev")).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "\n"})
	void 메시지_ID가_없으면_건너뛴다(String messageId) {
		assertThat(SupportReply.from("dev-42", messageId, "dev")).isEmpty();
	}
}
