package com.daesabu.meongcoach.training_comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training_comment.domain.exception.InvalidCommentContentException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardCommentTest {

	private static final Long CARD_ID = 1L;

	private static final Long AUTHOR_ID = 2L;

	@Test
	void 최상위_댓글은_본문의_앞뒤_공백을_제거하고_스레드_정보가_없다() {
		CardComment comment = CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "  안녕하세요.  "));

		assertThat(comment.getCardId()).isEqualTo(CARD_ID);
		assertThat(comment.getAuthorId()).isEqualTo(AUTHOR_ID);
		assertThat(comment.getContent()).isEqualTo("안녕하세요.");
		assertThat(comment.getRootId()).isNull();
		assertThat(comment.getParentId()).isNull();
		assertThat(comment.isReply()).isFalse();
	}

	@Test
	void 본문이_비어_있거나_500자를_넘으면_댓글을_만들_수_없다() {
		assertThatThrownBy(() -> CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, null)))
				.isInstanceOf(InvalidCommentContentException.class);
		assertThatThrownBy(() -> CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "  \t  ")))
				.isInstanceOf(InvalidCommentContentException.class);
		assertThatThrownBy(() -> CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "가".repeat(501))))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 최상위_댓글에_단_답글은_그_댓글을_스레드와_대상으로_삼는다() {
		CardComment root = persisted(CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "부모 댓글")), 10L);

		CardComment reply = CardComment.reply(root, 3L, " 첫째 답글 ");

		assertThat(reply.getCardId()).isEqualTo(CARD_ID);
		assertThat(reply.getAuthorId()).isEqualTo(3L);
		assertThat(reply.getContent()).isEqualTo("첫째 답글");
		assertThat(reply.getRootId()).isEqualTo(10L);
		assertThat(reply.getParentId()).isEqualTo(10L);
		assertThat(reply.isReply()).isTrue();
	}

	@Test
	void 답글에_단_답글은_같은_스레드에_붙고_답한_답글을_대상으로_남긴다() {
		CardComment root = persisted(CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "부모 댓글")), 10L);
		CardComment firstReply = persisted(CardComment.reply(root, 3L, "첫째 답글"), 11L);

		CardComment secondReply = CardComment.reply(firstReply, 4L, "둘째 답글");

		assertThat(secondReply.getCardId()).isEqualTo(CARD_ID);
		assertThat(secondReply.getRootId()).isEqualTo(10L);
		assertThat(secondReply.getParentId()).isEqualTo(11L);
	}

	@Test
	void 답글_본문도_같은_규칙으로_검증한다() {
		CardComment root = persisted(CardComment.create(new CardCommentCreateCommand(CARD_ID, AUTHOR_ID, "부모 댓글")), 10L);

		assertThatThrownBy(() -> CardComment.reply(root, 3L, "   "))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	private CardComment persisted(CardComment comment, Long id) {
		ReflectionTestUtils.setField(comment, "id", id);
		return comment;
	}
}
