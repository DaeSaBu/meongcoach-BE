package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import java.util.List;

public record CommentListResponse(List<CommentResponse> comments, Long nextCursor) {

	public static CommentListResponse from(CommentListResult result) {
		List<CommentResponse> comments = result.comments().stream()
				.map(CommentResponse::from)
				.toList();
		return new CommentListResponse(comments, result.nextCursor());
	}
}
