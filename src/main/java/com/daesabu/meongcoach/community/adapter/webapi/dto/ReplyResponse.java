package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.ReplyResult;
import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;
import java.util.Optional;

public record ReplyResponse(Long commentId, String content, AuthorResponse author, LocalDateTime createdAt) {

	public static ReplyResponse from(ReplyResult result) {
		Comment comment = result.comment();
		AuthorResponse author = Optional.ofNullable(result.author())
				.map(AuthorResponse::from)
				.orElse(null);
		return new ReplyResponse(comment.getId(), comment.getContent().value(), author, comment.getCreatedAt());
	}
}
