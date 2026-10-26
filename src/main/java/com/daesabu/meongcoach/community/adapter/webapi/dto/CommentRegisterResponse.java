package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;

public record CommentRegisterResponse(Long commentId, Long postId, Long userId, String content,
		LocalDateTime createdAt) {

	public static CommentRegisterResponse from(Comment comment) {
		return new CommentRegisterResponse(comment.getId(), comment.getPost().getId(), comment.getUserId(),
				comment.getContent().value(), comment.getCreatedAt());
	}
}
