package com.daesabu.meongcoach.training_comment.adapter.webapi.dto;

import com.daesabu.meongcoach.training_comment.application.provided.CommentPageResult;
import java.util.List;

public record CommentListResponse(List<CommentResponse> comments, long totalCount, Long nextCursor) {

	public static CommentListResponse from(CommentPageResult page, Long userId) {
		List<CommentResponse> comments = page.comments().stream()
				.map(result -> CommentResponse.from(result, userId))
				.toList();
		return new CommentListResponse(comments, page.totalCount(), page.nextCursor());
	}
}
