package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommentContentTest {

	@Test
	void 유효한_본문으로_생성에_성공한다() {
		CommentContent content = new CommentContent("저희 강아지도 성공했어요!");

		assertThat(content.value()).isEqualTo("저희 강아지도 성공했어요!");
	}

	@Test
	void 같은_본문끼리는_동등하다() {
		assertThat(new CommentContent("본문")).isEqualTo(new CommentContent("본문"));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   "})
	void 본문이_비어_있으면_생성에_실패한다(String value) {
		assertThatThrownBy(() -> new CommentContent(value))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 본문이_150자를_넘으면_생성에_실패한다() {
		String tooLong = "a".repeat(CommentContent.MAX_LENGTH + 1);

		assertThatThrownBy(() -> new CommentContent(tooLong))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 본문이_150자면_생성에_성공한다() {
		String maxLength = "a".repeat(CommentContent.MAX_LENGTH);

		CommentContent content = new CommentContent(maxLength);

		assertThat(content.value()).hasSize(CommentContent.MAX_LENGTH);
	}
}
