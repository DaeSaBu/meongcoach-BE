package com.daesabu.meongcoach.community.domain;

public record LikeNotification(String title, String body, String deepLink) {
	public static LikeNotification of(LikeRegisteredEvent event) {
		return new LikeNotification(null, event.targetKind() + "에 좋아요가 달렸어요", "meongcoach://community-post/" + event.postId());
	}
}
