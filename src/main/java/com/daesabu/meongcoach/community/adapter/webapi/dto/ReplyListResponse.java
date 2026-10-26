package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import java.util.List;

public record ReplyListResponse(List<ReplyResponse> replies, Long nextCursor) {

	public static ReplyListResponse from(ReplyListResult result) {
		List<ReplyResponse> replies = result.replies().stream()
				.map(ReplyResponse::from)
				.toList();
		return new ReplyListResponse(replies, result.nextCursor());
	}
}
