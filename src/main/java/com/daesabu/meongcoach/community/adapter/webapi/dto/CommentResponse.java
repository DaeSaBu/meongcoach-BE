package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;
import java.util.Optional;

public record CommentResponse(Long commentId, String content, AuthorResponse author, long replyCount,
		LocalDateTime createdAt) {

	public static CommentResponse from(CommentResult result) {
		Comment comment = result.comment();
		AuthorResponse author = Optional.ofNullable(result.author())
				.map(AuthorResponse::from)
				.orElse(null);
		return new CommentResponse(comment.getId(), comment.getContent().value(), author, result.replyCount(),
				comment.getCreatedAt());
	}
}
