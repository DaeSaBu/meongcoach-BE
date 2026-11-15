package com.daesabu.meongcoach.community.domain;

import java.util.List;

public record LikeRegisteredEvent(Long postId, Long authorId, Long likerId, String targetKind) {
	public List<Long> recipientIds() {
		if (authorId.equals(likerId)) {
			return List.of();
		}
		return List.of(authorId);
	}
}
