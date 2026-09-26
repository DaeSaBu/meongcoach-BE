package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import java.time.LocalDateTime;

public record ReplyResponse(Long id, String authorNickname, String content, LocalDateTime createdAt) {

	public static ReplyResponse from(ReplyResult result) {
		return new ReplyResponse(result.id(), result.authorNickname(), result.content(), result.createdAt());
	}
}
