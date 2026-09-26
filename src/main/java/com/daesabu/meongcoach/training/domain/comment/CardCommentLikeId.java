package com.daesabu.meongcoach.training.domain.comment;

import java.io.Serializable;
import java.util.Objects;

public class CardCommentLikeId implements Serializable {

	private Long commentId;
	private Long userId;

	public CardCommentLikeId() {
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CardCommentLikeId that)) {
			return false;
		}
		return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(commentId, userId);
	}
}
