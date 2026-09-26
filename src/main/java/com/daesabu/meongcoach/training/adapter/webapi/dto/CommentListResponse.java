package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CommentResult;
import java.util.List;

public record CommentListResponse(List<CommentResponse> comments) {

	public static CommentListResponse from(List<CommentResult> results) {
		List<CommentResponse> comments = results.stream()
				.map(CommentResponse::from)
				.toList();
		return new CommentListResponse(comments);
	}
}
