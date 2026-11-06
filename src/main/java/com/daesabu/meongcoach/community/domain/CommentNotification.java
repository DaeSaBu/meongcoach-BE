package com.daesabu.meongcoach.community.domain;

public record CommentNotification(String title, String body, String deepLink) {

	private static final String DEEP_LINK_PREFIX = "meongcoach://community-post/";
	private static final String COMMENT = "댓글";
	private static final String REPLY = "답글";

	public static CommentNotification of(CommentRegisteredEvent event) {
		String kind = kindOf(event);
		return new CommentNotification(
				null,
				kind + "이 달렸어요: " + event.content(),
				DEEP_LINK_PREFIX + event.postId());
	}

	private static String kindOf(CommentRegisteredEvent event) {
		if (event.isReply()) {
			return REPLY;
		}
		return COMMENT;
	}
}
