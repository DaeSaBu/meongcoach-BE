package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;

public record CommentUpdateResponse(Long commentId, Long postId, Long userId, String content, List<PostMediaResponse> media,
		LocalDateTime createdAt) {

	public static CommentUpdateResponse from(Comment comment) {
		return new CommentUpdateResponse(comment.getId(), comment.getPost().getId(), comment.getUserId(),
				comment.getContent().value(), PostMediaResponse.listOf(comment), comment.getCreatedAt());
	}
}
