package com.daesabu.meongcoach.training.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.domain.exception.InvalidCardCommentContentException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardCommentTest {

	@Test
	void 댓글_본문의_앞뒤_공백을_제거한다() {
		CardComment comment = CardComment.create(1L, 2L, "  안녕하세요.  ");

		assertThat(comment.getCardId()).isEqualTo(1L);
		assertThat(comment.getUserId()).isEqualTo(2L);
		assertThat(comment.getParentId()).isNull();
		assertThat(comment.getContent()).isEqualTo("안녕하세요.");
	}

	@Test
	void 본문이_비어_있거나_500자를_넘으면_댓글을_만들_수_없다() {
		assertThatThrownBy(() -> CardComment.create(1L, 2L, null))
				.isInstanceOf(InvalidCardCommentContentException.class);
		assertThatThrownBy(() -> CardComment.create(1L, 2L, "  \t  "))
				.isInstanceOf(InvalidCardCommentContentException.class);
		assertThatThrownBy(() -> CardComment.create(1L, 2L, "가".repeat(501)))
				.isInstanceOf(InvalidCardCommentContentException.class);
	}

	@Test
	void 답글은_대상_댓글의_카드와_최상위_부모를_따른다() {
		CardComment parent = CardComment.create(1L, 2L, "부모 댓글");
		ReflectionTestUtils.setField(parent, "id", 10L);
		CardComment firstReply = CardComment.createReply(parent, 3L, " 첫째 답글 ");
		ReflectionTestUtils.setField(firstReply, "id", 11L);

		CardComment secondReply = CardComment.createReply(firstReply, 4L, " 둘째 답글 ");

		assertThat(firstReply.getCardId()).isEqualTo(parent.getCardId());
		assertThat(firstReply.getParentId()).isEqualTo(parent.getId());
		assertThat(firstReply.getContent()).isEqualTo("첫째 답글");
		assertThat(secondReply.getCardId()).isEqualTo(parent.getCardId());
		assertThat(secondReply.getParentId()).isEqualTo(parent.getId());
		assertThat(secondReply.getUserId()).isEqualTo(4L);
	}
}
