package com.daesabu.meongcoach.cardcomment.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * 댓글 좋아요의 복합 식별자. JPA IdClass 규약상 기본 생성자와 equals·hashCode가 필요하다.
 */
public class CardCommentLikeId implements Serializable {

	private Long commentId;

	private Long userId;

	protected CardCommentLikeId() {
	}

	public CardCommentLikeId(Long commentId, Long userId) {
		this.commentId = commentId;
		this.userId = userId;
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
