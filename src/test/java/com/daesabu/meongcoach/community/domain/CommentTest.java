package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CommentTest {

	private static final CommentTarget CARD = CommentTarget.card(1L);

	private static final Long AUTHOR_ID = 2L;

	@Test
	void 최상위_댓글은_본문의_앞뒤_공백을_제거하고_스레드_정보가_없다() {
		Comment comment = Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "  안녕하세요.  "));

		assertThat(comment.getTarget()).isEqualTo(new CommentTarget(CommentTargetType.CARD, 1L));
		assertThat(comment.getAuthorId()).isEqualTo(AUTHOR_ID);
		assertThat(comment.getContent()).isEqualTo("안녕하세요.");
		assertThat(comment.getRootId()).isNull();
		assertThat(comment.getParentId()).isNull();
		assertThat(comment.isReply()).isFalse();
	}

	@Test
	void 본문이_비어_있거나_500자를_넘으면_댓글을_만들_수_없다() {
		assertThatThrownBy(() -> Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, null)))
				.isInstanceOf(InvalidCommentContentException.class);
		assertThatThrownBy(() -> Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "  \t  ")))
				.isInstanceOf(InvalidCommentContentException.class);
		assertThatThrownBy(() -> Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "가".repeat(501))))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 최상위_댓글에_단_답글은_그_댓글을_스레드와_대상으로_삼는다() {
		Comment root = persisted(Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "부모 댓글")), 10L);

		Comment reply = Comment.reply(root, 3L, " 첫째 답글 ");

		assertThat(reply.getTarget()).isEqualTo(CARD);
		assertThat(reply.getAuthorId()).isEqualTo(3L);
		assertThat(reply.getContent()).isEqualTo("첫째 답글");
		assertThat(reply.getRootId()).isEqualTo(10L);
		assertThat(reply.getParentId()).isEqualTo(10L);
		assertThat(reply.isReply()).isTrue();
	}

	@Test
	void 답글에_단_답글은_같은_스레드에_붙고_답한_답글을_대상으로_남긴다() {
		Comment root = persisted(Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "부모 댓글")), 10L);
		Comment firstReply = persisted(Comment.reply(root, 3L, "첫째 답글"), 11L);

		Comment secondReply = Comment.reply(firstReply, 4L, "둘째 답글");

		assertThat(secondReply.getTarget()).isEqualTo(CARD);
		assertThat(secondReply.getRootId()).isEqualTo(10L);
		assertThat(secondReply.getParentId()).isEqualTo(11L);
	}

	@Test
	void 답글_본문도_같은_규칙으로_검증한다() {
		Comment root = persisted(Comment.create(new CommentCreateCommand(CARD, AUTHOR_ID, "부모 댓글")), 10L);

		assertThatThrownBy(() -> Comment.reply(root, 3L, "   "))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	private Comment persisted(Comment comment, Long id) {
		ReflectionTestUtils.setField(comment, "id", id);
		return comment;
	}
}
