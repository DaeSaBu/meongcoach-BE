package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.comment.CardCommentLike;
import java.util.List;

public record CommentLikeResult(Long commentId, long likeCount, boolean likedByMe) {

	public static CommentLikeResult from(List<CardCommentLike> likes, Long userId) {
		Long commentId = likes.getFirst().getCommentId();
		boolean likedByMe = likes.stream().anyMatch(like -> like.getUserId().equals(userId));
		return new CommentLikeResult(commentId, likes.size(), likedByMe);
	}
}
