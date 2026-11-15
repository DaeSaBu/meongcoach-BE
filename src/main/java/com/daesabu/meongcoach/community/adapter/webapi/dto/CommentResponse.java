package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.domain.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record CommentResponse(Long commentId, String content, List<PostMediaResponse> media, AuthorResponse author, long replyCount,
		LocalDateTime createdAt, long likeCount, boolean likedByMe) {

	public static CommentResponse from(CommentResult result) {
		Comment comment = result.comment();
		AuthorResponse author = Optional.ofNullable(result.author())
				.map(AuthorResponse::from)
				.orElse(null);
		return new CommentResponse(comment.getId(), comment.getContent().value(), PostMediaResponse.listOf(comment), author, result.replyCount(),
				comment.getCreatedAt(), result.likeCount(), result.likedByMe());
	}
}
