package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;
import java.util.List;

public record CommentLikesResponse(List<CommentLikeStateResponse> likes) {

	public static CommentLikesResponse from(List<CommentLikeResult> results) {
		List<CommentLikeStateResponse> likes = results.stream()
				.map(CommentLikeStateResponse::from)
				.toList();
		return new CommentLikesResponse(likes);
	}
}
