package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CommentRegisteredEventTest {

	private static final Long POST_AUTHOR_ID = 1L;
	private static final Long ROOT_AUTHOR_ID = 2L;
	private static final Long COMMENTER_ID = 3L;

	@Test
	void 루트_댓글이면_게시글_작성자에게만_알린다() {
		CommentRegisteredEvent event = event(POST_AUTHOR_ID, COMMENTER_ID, null);

		assertThat(event.isReply()).isFalse();
		assertThat(event.recipientIds()).containsExactly(POST_AUTHOR_ID);
	}

	@Test
	void 답글이면_게시글_작성자와_루트_댓글_작성자에게_알린다() {
		CommentRegisteredEvent event = event(POST_AUTHOR_ID, COMMENTER_ID, ROOT_AUTHOR_ID);

		assertThat(event.isReply()).isTrue();
		assertThat(event.recipientIds()).containsExactly(POST_AUTHOR_ID, ROOT_AUTHOR_ID);
	}

	@Test
	void 댓글_작성자_본인은_수신자에서_뺀다() {
		CommentRegisteredEvent ownPost = event(COMMENTER_ID, COMMENTER_ID, null);
		CommentRegisteredEvent ownRoot = event(POST_AUTHOR_ID, COMMENTER_ID, COMMENTER_ID);

		assertThat(ownPost.recipientIds()).isEmpty();
		assertThat(ownRoot.recipientIds()).containsExactly(POST_AUTHOR_ID);
	}

	@Test
	void 게시글_작성자가_루트_댓글_작성자와_같으면_한_번만_알린다() {
		CommentRegisteredEvent event = event(POST_AUTHOR_ID, COMMENTER_ID, POST_AUTHOR_ID);

		assertThat(event.recipientIds()).containsExactly(POST_AUTHOR_ID);
	}

	@Test
	void 댓글과_게시글에서_작성자와_본문을_옮긴다() {
		Post post = Post.register(POST_AUTHOR_ID, new PostRegisterCommand("오늘 앉아 훈련 성공!", null, List.of()));
		Comment root = Comment.register(post, ROOT_AUTHOR_ID, "축하해요!");
		Comment reply = Comment.register(post, COMMENTER_ID, "저도 축하해요!");

		CommentRegisteredEvent rootEvent = CommentRegisteredEvent.from(root, null);
		CommentRegisteredEvent replyEvent = CommentRegisteredEvent.from(reply, root);

		assertThat(rootEvent.postAuthorId()).isEqualTo(POST_AUTHOR_ID);
		assertThat(rootEvent.commenterId()).isEqualTo(ROOT_AUTHOR_ID);
		assertThat(rootEvent.rootCommentAuthorId()).isNull();
		assertThat(rootEvent.content()).isEqualTo("축하해요!");
		assertThat(replyEvent.commenterId()).isEqualTo(COMMENTER_ID);
		assertThat(replyEvent.rootCommentAuthorId()).isEqualTo(ROOT_AUTHOR_ID);
		assertThat(replyEvent.content()).isEqualTo("저도 축하해요!");
	}

	private CommentRegisteredEvent event(Long postAuthorId, Long commenterId, Long rootCommentAuthorId) {
		return new CommentRegisteredEvent(10L, 20L, postAuthorId, commenterId, rootCommentAuthorId, "축하해요!");
	}
}
