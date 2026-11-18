package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.MyCommentListResult;
import java.util.List;

public record MyCommentListResponse(List<MyCommentResponse> comments, Long nextCursor) {

	public static MyCommentListResponse from(MyCommentListResult result) {
		List<MyCommentResponse> comments = result.comments().stream()
				.map(MyCommentResponse::from)
				.toList();
		return new MyCommentListResponse(comments, result.nextCursor());
	}
}
