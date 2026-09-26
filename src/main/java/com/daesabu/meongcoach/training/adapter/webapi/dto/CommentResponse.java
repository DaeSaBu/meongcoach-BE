package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CommentResult;
import java.time.LocalDateTime;

public record CommentResponse(Long id, String authorNickname, String content, LocalDateTime createdAt,
	                              long replyCount) {

	public static CommentResponse from(CommentResult result) {
		return new CommentResponse(result.id(), result.authorNickname(), result.content(), result.createdAt(),
				result.replyCount());
	}
}
