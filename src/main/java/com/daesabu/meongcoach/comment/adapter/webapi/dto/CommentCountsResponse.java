package com.daesabu.meongcoach.comment.adapter.webapi.dto;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;
import java.util.List;

public record CommentCountsResponse(List<CardCommentCountResponse> counts) {

	public static CommentCountsResponse from(List<CardCommentCountResult> results) {
		List<CardCommentCountResponse> counts = results.stream()
				.map(CardCommentCountResponse::from)
				.toList();
		return new CommentCountsResponse(counts);
	}
}
