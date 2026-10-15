package com.daesabu.meongcoach.ai.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import com.daesabu.meongcoach.ai.domain.shared.StructuredChatCompletionFailedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class StructuredChatCompleterTest {

	private static final StructuredChatRequest REQUEST = new StructuredChatRequest("시스템", "사용자", "plan",
			"{\"type\":\"object\"}");

	@Autowired
	private StructuredChatCompleter structuredChatCompleter;

	@Autowired
	private StructuredChatClient structuredChatClient;

	@Test
	void 요청을_포트에_위임하고_응답_JSON을_그대로_반환한다() {
		given(structuredChatClient.complete(REQUEST)).willReturn("{\"ok\":true}");

		String json = structuredChatCompleter.complete(REQUEST);

		assertThat(json).isEqualTo("{\"ok\":true}");
	}

	@Test
	void 포트가_실패하면_예외를_그대로_전파한다() {
		given(structuredChatClient.complete(REQUEST)).willThrow(new StructuredChatCompletionFailedException("실패"));

		assertThatThrownBy(() -> structuredChatCompleter.complete(REQUEST))
				.isInstanceOf(StructuredChatCompletionFailedException.class);
	}
}
