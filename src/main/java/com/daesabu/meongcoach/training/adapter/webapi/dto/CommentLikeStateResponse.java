package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;

public record CommentLikeStateResponse(Long commentId, long likeCount, boolean likedByMe) {

	public static CommentLikeStateResponse from(CommentLikeResult result) {
		return new CommentLikeStateResponse(result.commentId(), result.likeCount(), result.likedByMe());
	}
}
