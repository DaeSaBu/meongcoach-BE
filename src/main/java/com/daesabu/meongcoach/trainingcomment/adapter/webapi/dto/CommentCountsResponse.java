package com.daesabu.meongcoach.trainingcomment.adapter.webapi.dto;

import com.daesabu.meongcoach.trainingcomment.application.provided.CardCommentCountResult;
import java.util.List;

public record CommentCountsResponse(List<CardCommentCountResponse> counts) {

	public static CommentCountsResponse from(List<CardCommentCountResult> results) {
		List<CardCommentCountResponse> counts = results.stream()
				.map(CardCommentCountResponse::from)
				.toList();
		return new CommentCountsResponse(counts);
	}
}
