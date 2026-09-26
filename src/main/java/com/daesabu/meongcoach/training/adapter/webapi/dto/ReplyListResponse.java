package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import java.util.List;

public record ReplyListResponse(List<ReplyResponse> replies) {

	public static ReplyListResponse from(List<ReplyResult> results) {
		List<ReplyResponse> replies = results.stream()
				.map(ReplyResponse::from)
				.toList();
		return new ReplyListResponse(replies);
	}
}
