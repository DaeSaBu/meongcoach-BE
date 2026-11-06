package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentNotificationTest {

	private static final Long POST_ID = 20L;

	@Test
	void 루트_댓글은_본문으로_댓글_문구를_만든다() {
		CommentNotification notification = CommentNotification.of(event(null));

		assertThat(notification.title()).isNull();
		assertThat(notification.body()).isEqualTo("댓글이 달렸어요: 축하해요!");
	}

	@Test
	void 답글은_답글_문구를_만든다() {
		CommentNotification notification = CommentNotification.of(event(2L));

		assertThat(notification.title()).isNull();
		assertThat(notification.body()).isEqualTo("답글이 달렸어요: 축하해요!");
	}

	@Test
	void 딥링크는_게시글_화면을_가리킨다() {
		CommentNotification notification = CommentNotification.of(event(null));

		assertThat(notification.deepLink()).isEqualTo("meongcoach://community-post/20");
	}

	private CommentRegisteredEvent event(Long rootCommentAuthorId) {
		return new CommentRegisteredEvent(10L, POST_ID, 1L, 3L, rootCommentAuthorId, "축하해요!");
	}
}
