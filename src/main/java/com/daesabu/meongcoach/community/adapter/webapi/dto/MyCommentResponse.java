package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record MyCommentResponse(Long commentId, Long postId, String postContent, String content, List<PostMediaResponse> media,
		LocalDateTime createdAt, long likeCount, boolean likedByMe) {

	public static MyCommentResponse from(CommentResult result) {
		Comment comment = result.comment();
		Post post = comment.getPost();
		return new MyCommentResponse(comment.getId(), post.getId(), post.getContent().value(), comment.getContent().value(),
				PostMediaResponse.listOf(comment), comment.getCreatedAt(), result.likeCount(), result.likedByMe());
	}
}
