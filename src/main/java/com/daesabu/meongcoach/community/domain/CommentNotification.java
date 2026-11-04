package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNullElse;

public record CommentNotification(String title, String body, String deepLink) {

	private static final String DEFAULT_NICKNAME = "회원";
	private static final String DEEP_LINK_PREFIX = "meongcoach://community-post/";
	private static final String COMMENT = "댓글";
	private static final String REPLY = "답글";

	public static CommentNotification of(CommentRegisteredEvent event, String nickname) {
		String kind = kindOf(event);
		String author = requireNonNullElse(nickname, DEFAULT_NICKNAME);
		return new CommentNotification(
				null,
				author + "님이 " + kind + "을 남겼어요: " + event.content(),
				DEEP_LINK_PREFIX + event.postId());
	}

	private static String kindOf(CommentRegisteredEvent event) {
		if (event.isReply()) {
			return REPLY;
		}
		return COMMENT;
	}
}
