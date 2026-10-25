package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PostContentTest {

	@Test
	void 유효한_본문으로_생성에_성공한다() {
		PostContent content = new PostContent("오늘 앉아 훈련 성공!");

		assertThat(content.value()).isEqualTo("오늘 앉아 훈련 성공!");
	}

	@Test
	void 같은_본문끼리는_동등하다() {
		assertThat(new PostContent("본문")).isEqualTo(new PostContent("본문"));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   "})
	void 본문이_비어_있으면_생성에_실패한다(String value) {
		assertThatThrownBy(() -> new PostContent(value))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 본문이_200자를_넘으면_생성에_실패한다() {
		String tooLong = "a".repeat(PostContent.MAX_LENGTH + 1);

		assertThatThrownBy(() -> new PostContent(tooLong))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 본문이_200자면_생성에_성공한다() {
		String maxLength = "a".repeat(PostContent.MAX_LENGTH);

		PostContent content = new PostContent(maxLength);

		assertThat(content.value()).hasSize(PostContent.MAX_LENGTH);
	}
}
