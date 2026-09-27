package com.daesabu.meongcoach.trainingcomment.adapter.webapi.dto;

import com.daesabu.meongcoach.trainingcomment.application.provided.CommentResult;
import java.time.LocalDateTime;

public record CommentResponse(Long id, Long parentId, Long authorId, String authorNickname, boolean mine,
                              String content, LocalDateTime createdAt, long replyCount, long likeCount,
                              boolean likedByMe) {

	public static CommentResponse from(CommentResult result, Long userId) {
		return new CommentResponse(result.id(), result.parentId(), result.authorId(), result.authorNickname(),
				result.authorId().equals(userId), result.content(), result.createdAt(), result.replyCount(),
				result.likeCount(), result.likedByMe());
	}
}
