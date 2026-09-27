package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.CommentCountResult;
import java.util.List;

public record CommentCountsResponse(List<CardCommentCountResponse> counts) {

	public static CommentCountsResponse from(List<CommentCountResult> results) {
		List<CardCommentCountResponse> counts = results.stream()
				.map(CardCommentCountResponse::from)
				.toList();
		return new CommentCountsResponse(counts);
	}
}
