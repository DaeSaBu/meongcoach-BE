package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;

public record CommentRegisterResponse(Long commentId, Long postId, Long userId, String content, List<PostMediaResponse> media,
		LocalDateTime createdAt) {

	public static CommentRegisterResponse from(Comment comment) {
		return new CommentRegisterResponse(comment.getId(), comment.getPost().getId(), comment.getUserId(),
				comment.getContent().value(), PostMediaResponse.listOf(comment), comment.getCreatedAt());
	}
}
