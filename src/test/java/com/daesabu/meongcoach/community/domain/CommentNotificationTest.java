package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentNotificationTest {

	private static final Long POST_ID = 20L;

	@Test
	void 루트_댓글은_닉네임과_본문으로_댓글_문구를_만든다() {
		CommentNotification notification = CommentNotification.of(event(null), "멍멍이집사");

		assertThat(notification.title()).isNull();
		assertThat(notification.body()).isEqualTo("멍멍이집사님이 댓글을 남겼어요: 축하해요!");
	}

	@Test
	void 답글은_답글_문구를_만든다() {
		CommentNotification notification = CommentNotification.of(event(2L), "멍멍이집사");

		assertThat(notification.title()).isNull();
		assertThat(notification.body()).isEqualTo("멍멍이집사님이 답글을 남겼어요: 축하해요!");
	}

	@Test
	void 닉네임이_없으면_기본_호칭을_쓴다() {
		CommentNotification notification = CommentNotification.of(event(null), null);

		assertThat(notification.body()).isEqualTo("회원님이 댓글을 남겼어요: 축하해요!");
	}

	@Test
	void 딥링크는_게시글_화면을_가리킨다() {
		CommentNotification notification = CommentNotification.of(event(null), "멍멍이집사");

		assertThat(notification.deepLink()).isEqualTo("meongcoach://community-post/20");
	}

	private CommentRegisteredEvent event(Long rootCommentAuthorId) {
		return new CommentRegisteredEvent(10L, POST_ID, 1L, 3L, rootCommentAuthorId, "축하해요!");
	}
}
