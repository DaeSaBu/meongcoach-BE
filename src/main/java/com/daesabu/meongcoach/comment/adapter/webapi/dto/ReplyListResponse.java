package com.daesabu.meongcoach.comment.adapter.webapi.dto;

import com.daesabu.meongcoach.comment.application.provided.ReplyPageResult;
import java.util.List;

public record ReplyListResponse(List<CommentResponse> replies, Long nextCursor) {

	public static ReplyListResponse from(ReplyPageResult page, Long userId) {
		List<CommentResponse> replies = page.replies().stream()
				.map(result -> CommentResponse.from(result, userId))
				.toList();
		return new ReplyListResponse(replies, page.nextCursor());
	}
}
