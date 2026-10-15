package com.daesabu.meongcoach.ai.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.ai.domain.exception.AiErrorCode;
import com.daesabu.meongcoach.shared.exception.DomainException;
import org.junit.jupiter.api.Test;

class StructuredChatCompletionFailedExceptionTest {

	@Test
	void 구조화_채팅에_실패하면_AI_STRUCTURED_CHAT_FAILED_코드를_갖는다() {
		DomainException exception = new StructuredChatCompletionFailedException("모델 호출 실패");

		assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_STRUCTURED_CHAT_FAILED);
		assertThat(exception.getErrorCode().status()).isEqualTo(502);
	}

	@Test
	void 원인_예외를_보존한다() {
		IllegalStateException cause = new IllegalStateException("원인");

		DomainException exception = new StructuredChatCompletionFailedException("모델 호출 실패", cause);

		assertThat(exception.getCause()).isSameAs(cause);
	}
}
